package com.github.l34130.netty.dbgw.protocol.mysql.connection

import com.github.l34130.netty.dbgw.core.utils.netty.closeOnFlush
import com.github.l34130.netty.dbgw.core.utils.toEnumSet
import com.github.l34130.netty.dbgw.protocol.mysql.GatewayState
import com.github.l34130.netty.dbgw.protocol.mysql.Packet
import com.github.l34130.netty.dbgw.protocol.mysql.capabilities
import com.github.l34130.netty.dbgw.protocol.mysql.constant.CapabilityFlag
import com.github.l34130.netty.dbgw.protocol.mysql.readFixedLengthInteger
import com.github.l34130.netty.dbgw.protocol.mysql.readLenEncInteger
import com.github.l34130.netty.dbgw.protocol.mysql.readLenEncString
import com.github.l34130.netty.dbgw.protocol.mysql.readNullTerminatedString
import com.github.l34130.netty.dbgw.protocol.mysql.upstream
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.SslHandler
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import java.util.EnumSet

class HandshakeResponseState : GatewayState {
    override fun onDownstreamPacket(
        ctx: ChannelHandlerContext,
        packet: Packet,
    ): GatewayState {
        val payload = packet.payload
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

        val downstream = ctx.channel()
        val upstream = ctx.upstream()
        if (ctx.capabilities().contains(CapabilityFlag.CLIENT_SSL) && downstream.pipeline().get("ssl-handler") == null) {
            // TODO: Singletonize SSL context creation
            val serverSslContext =
                SslContextBuilder
                    .forServer(
                        this.javaClass.classLoader.getResourceAsStream("certificate.pem"),
                        this.javaClass.classLoader.getResourceAsStream("private.key"),
                    ).build()
                    .newEngine(downstream.alloc())

            // TODO: Use InsecureTrustManagerFactory for testing purposes only
            val clientSslContext =
                SslContextBuilder
                    .forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .build()
                    .newEngine(upstream.alloc())

            payload.resetReaderIndex()
            upstream.writeAndFlush(packet)
            upstream.pipeline().addFirst(
                "ssl-handler",
                SslHandler(clientSslContext).apply {
                    handshakeFuture().addListener { future ->
                        if (!future.isSuccess) {
                            logger.error(future.cause()) { "[Upstream] SSL handshake failed." }
                            downstream.closeOnFlush()
                            return@addListener
                        }

                        logger.info { "[Upstream] SSL handshake completed successfully." }
                    }
                    upstream.writeAndFlush(Unpooled.EMPTY_BUFFER) // Trigger the SSL handshake
                },
            )

            downstream.pipeline().addFirst(
                "ssl-handler",
                SslHandler(serverSslContext).apply {
                    handshakeFuture().addListener { future ->
                        if (!future.isSuccess) {
                            logger.error(future.cause()) { "[Downstream] SSL handshake failed." }
                            downstream.closeOnFlush()
                            return@addListener
                        }

                        logger.info { "[Downstream] SSL handshake completed successfully." }
                    }
                },
            )

            return this // Wait for the HandshakeResponse packet again
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
                payload.readNullTerminatedString()
            } else {
                null
            }
        logger.trace { "Database: ${database?.toString(Charsets.US_ASCII)}" }

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

        val clientConnectAttrs =
            if (clientCapabilities.contains(CapabilityFlag.CLIENT_CONNECT_ATTRS)) {
                val lengthOfAllKeyValues = payload.readLenEncInteger()

                val keyValuesByteBuf = payload.readSlice(lengthOfAllKeyValues.toInt())

                val attrs = mutableListOf<Pair<String, String>>()
                while (keyValuesByteBuf.readableBytes() > 0) {
                    val key = keyValuesByteBuf.readLenEncString().toString(Charsets.UTF_8)
                    val value = keyValuesByteBuf.readLenEncString().toString(Charsets.UTF_8)
                    attrs.add(key to value)
                }
                attrs
            } else {
                emptyList()
            }
        logger.trace { "Client Connect Attributes: $clientConnectAttrs" }

        val zstdCompressionLevel =
            if (clientCapabilities.contains(CapabilityFlag.CLIENT_ZSTD_COMPRESSION_ALGORITHM)) {
                payload.readFixedLengthInteger(1).toInt()
            } else {
                0
            }
        logger.trace { "Zstd Compression Level: $zstdCompressionLevel" }

        payload.resetReaderIndex()
        ctx.upstream().writeAndFlush(packet)
        return AuthResultState()
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
