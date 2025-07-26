package com.github.l34130.netty.dbgw.protocol.mysql.connection

import com.github.l34130.netty.dbgw.core.MessageAction
import com.github.l34130.netty.dbgw.core.backend
import com.github.l34130.netty.dbgw.core.databaseCtx
import com.github.l34130.netty.dbgw.core.utils.netty.closeOnFlush
import com.github.l34130.netty.dbgw.core.utils.toEnumSet
import com.github.l34130.netty.dbgw.protocol.mysql.MySqlGatewayState
import com.github.l34130.netty.dbgw.protocol.mysql.Packet
import com.github.l34130.netty.dbgw.protocol.mysql.capabilities
import com.github.l34130.netty.dbgw.protocol.mysql.constant.CapabilityFlag
import com.github.l34130.netty.dbgw.protocol.mysql.readFixedLengthInteger
import com.github.l34130.netty.dbgw.protocol.mysql.readLenEncInteger
import com.github.l34130.netty.dbgw.protocol.mysql.readLenEncString
import com.github.l34130.netty.dbgw.protocol.mysql.readNullTerminatedString
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.SslHandler
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import java.util.EnumSet

internal class HandshakeResponseState : MySqlGatewayState() {
    override fun onFrontendMessage(
        ctx: ChannelHandlerContext,
        msg: Packet,
    ): StateResult {
        val payload = msg.payload
        payload.markReaderIndex()

        val clientFlag = payload.readFixedLengthInteger(4)
        val clientCapabilities: EnumSet<CapabilityFlag> = clientFlag.toEnumSet()
        ctx.capabilities().setClientCapabilities(clientCapabilities)
        if (!clientCapabilities.contains(CapabilityFlag.CLIENT_PROTOCOL_41)) {
            logger.error { "Unsupported MySQL client protocol version: $clientFlag" }
            ctx.close()
            TODO("Handle unsupported client protocol version")
        }
        logger.trace { "Client Capabilities: $clientCapabilities" }

        val maxPacketSize = payload.readFixedLengthInteger(4)
        logger.trace { "Max Packet Size: $maxPacketSize bytes" }
        val characterSet =
            when (val v = payload.readFixedLengthInteger(1).toInt()) {
                1 -> "big5_chinese_ci"
                2 -> "latin2_czech_cs"
                3 -> "dec8_swedish_ci"
                4 -> "cp850_general_ci"
                5 -> "latin1_german1_ci"
                6 -> "hp8_english_ci"
                7 -> "koi8_ru_general_ci"
                8 -> "latin1_swedish_ci"
                9 -> "latin2_general_ci"
                10 -> "swe7_swedish_ci"
                33 -> "utf8mb3_general_ci"
                63 -> "binary"
                else -> "unknown_character_set($v)"
            }
        logger.trace { "Character Set: $characterSet" }
        payload.skipBytes(23) // skip filler bytes

        val frontend = ctx.channel()
        val backend = ctx.backend()
        if (ctx.capabilities().contains(CapabilityFlag.CLIENT_SSL) && frontend.pipeline().get("ssl-handler") == null) {
            // TODO: Singletonize SSL context creation
            val serverSslContext =
                SslContextBuilder
                    .forServer(
                        this.javaClass.classLoader.getResourceAsStream("certificate.pem"),
                        this.javaClass.classLoader.getResourceAsStream("private.key"),
                    ).build()
                    .newEngine(frontend.alloc())

            // TODO: Use InsecureTrustManagerFactory for testing purposes only
            val clientSslContext =
                SslContextBuilder
                    .forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .build()
                    .newEngine(backend.alloc())

            payload.resetReaderIndex()
            backend.writeAndFlush(msg)
            backend.pipeline().addFirst(
                "ssl-handler",
                SslHandler(clientSslContext).apply {
                    handshakeFuture().addListener { future ->
                        if (!future.isSuccess) {
                            logger.error(future.cause()) { "[Backend] SSL handshake failed." }
                            frontend.closeOnFlush()
                            return@addListener
                        }

                        logger.info { "[Backend] SSL handshake completed successfully." }
                    }
                    backend.writeAndFlush(Unpooled.EMPTY_BUFFER) // Trigger the SSL handshake
                },
            )

            frontend.pipeline().addFirst(
                "ssl-handler",
                SslHandler(serverSslContext).apply {
                    handshakeFuture().addListener { future ->
                        if (!future.isSuccess) {
                            logger.error(future.cause()) { "[Frontend] SSL handshake failed." }
                            frontend.closeOnFlush()
                            return@addListener
                        }

                        logger.info { "[Frontend] SSL handshake completed successfully." }
                    }
                },
            )

            return StateResult(
                nextState = this, // Wait for the HandshakeResponse packet again
                action = MessageAction.Drop, // Drop the original packet as we are handled it with SSL
            )
        }

        // login username
        val username = payload.readNullTerminatedString()
        logger.trace { "Username: ${username.toString(Charsets.US_ASCII)}" }

        val authResponse =
            if (clientCapabilities.contains(CapabilityFlag.CLIENT_PLUGIN_AUTH_LENENC_CLIENT_DATA)) {
                payload.readLenEncString()
            } else {
                val authResponseLength = payload.readFixedLengthInteger(1).toInt()
                payload.readLenEncString()
            }

        val database =
            if (clientCapabilities.contains(CapabilityFlag.CLIENT_CONNECT_WITH_DB)) {
                payload.readNullTerminatedString().toString(Charsets.US_ASCII)
            } else {
                null
            }
        logger.trace { "Database: $database" }
        ctx.databaseCtx()!!.connectionInfo.database = database

        val clientPluginName =
            if (clientCapabilities.contains(CapabilityFlag.CLIENT_PLUGIN_AUTH)) {
                payload.readNullTerminatedString()
            } else {
                null
            }
        logger.trace {
            "Client Auth Plugin Name: ${clientPluginName?.toString(
                Charsets.UTF_8,
            )}"
        }

        val clientConnectAttrs: Map<String, String> =
            if (clientCapabilities.contains(CapabilityFlag.CLIENT_CONNECT_ATTRS)) {
                val lengthOfAllKeyValues = payload.readLenEncInteger()

                val keyValuesByteBuf = payload.readSlice(lengthOfAllKeyValues.toInt())

                val attrs = mutableMapOf<String, String>()
                while (keyValuesByteBuf.readableBytes() > 0) {
                    val key = keyValuesByteBuf.readLenEncString().toString(Charsets.UTF_8)
                    val value = keyValuesByteBuf.readLenEncString().toString(Charsets.UTF_8)
                    attrs.put(key, value)
                }
                attrs
            } else {
                emptyMap()
            }
        logger.trace { "Client Connect Attributes: $clientConnectAttrs" }
        val clientName = clientNameFromConnectAttrs(clientConnectAttrs)
        ctx.databaseCtx()!!.clientInfo.userAgent = clientName

        val zstdCompressionLevel =
            if (clientCapabilities.contains(CapabilityFlag.CLIENT_ZSTD_COMPRESSION_ALGORITHM)) {
                payload.readFixedLengthInteger(1).toInt()
            } else {
                0
            }
        logger.trace { "Zstd Compression Level: $zstdCompressionLevel" }

        return StateResult(
            nextState = AuthResultState(),
            action = MessageAction.Forward,
        )
    }

    private fun clientNameFromConnectAttrs(clientConnectAttrs: Map<String, String>): String? {
        val runtimeVendor = clientConnectAttrs["_runtime_vendor"]
        var clientName = clientConnectAttrs["_client_name"]
        val clientVersion = clientConnectAttrs["_client_version"]
        val (program, version, comment) =
            if (runtimeVendor == "JetBrains s.r.o.") {
                val program = clientName ?: "JetBrains Client"
                val version = clientVersion
                val comment =
                    buildString {
                        append("JetBrains s.r.o.")
                        clientConnectAttrs["_runtime_version"]?.let { append("; $it") }
                    }

                Triple(program, version, comment)
            } else if (clientName == "libmysql") {
                val program = clientConnectAttrs["program_name"] ?: "mysql"
                val version = clientVersion
                val comment =
                    buildString {
                        append("libmysql")
                        clientConnectAttrs["_os"]?.let { append("; $it") }
                        clientConnectAttrs["_platform"]?.let { append("; $it") }
                    }

                Triple(program, version, comment)
            } else {
                logger.warn { "Failed to parse client from attrs: $clientConnectAttrs" }
                return null // Unknown client
            }
        return buildString {
            append(program)
            version?.let { append("/$it") }
            append(" ($comment)")
        }
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
