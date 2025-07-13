package com.github.l34130.netty.dbgw.app.handler.codec.mysql.connection

import com.github.l34130.netty.dbgw.app.handler.codec.mysql.Packet
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.ProxyContext
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.constant.CapabilityFlag
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.readFixedLengthInteger
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.readLenEncInteger
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.readLenEncString
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.readNullTerminatedString
import com.github.l34130.netty.dbgw.utils.netty.closeOnFlush
import com.github.l34130.netty.dbgw.utils.toEnumSet
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.SslHandler
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import io.netty.util.ReferenceCountUtil
import java.io.File
import java.util.EnumSet

class InitialHandshakeResponseHandler(
    private val proxyContext: ProxyContext,
) : SimpleChannelInboundHandler<Packet>() {
    override fun channelRead0(
        ctx: ChannelHandlerContext,
        msg: Packet,
    ) {
        val payload = msg.payload
        payload.markReaderIndex()

        val clientFlag = payload.readFixedLengthInteger(4).toInt()
        val clientCapabilities: EnumSet<CapabilityFlag> = clientFlag.toEnumSet()
        proxyContext.setClientCapabilities(clientCapabilities)
        if (!clientCapabilities.contains(CapabilityFlag.CLIENT_PROTOCOL_41)) {
            logger.error { "Unsupported MySQL client protocol version: $clientFlag" }
            ctx.close()
            return
        }

        val maxPacketSize = payload.readFixedLengthInteger(4)
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

        if (proxyContext.capabilities().contains(CapabilityFlag.CLIENT_SSL) && ctx.pipeline().get("ssl-handler") == null) {
            logger.trace { "Client supports SSL" }

            val downstream = proxyContext.downstream()
            val upstream = proxyContext.upstream()

            // TODO: Singletonize SSL context creation
            val serverSslContext =
                SslContextBuilder
                    .forServer(File("certificate.pem"), File("private.key"))
                    .build()
                    .newEngine(downstream.alloc())

            // TODO: Use InsecureTrustManagerFactory for testing purposes only
            val clientSslContext =
                SslContextBuilder
                    .forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .build()
                    .newEngine(upstream.alloc())

            payload.resetReaderIndex()
            upstream.writeAndFlush(msg).addListener { future ->
                if (!future.isSuccess) {
                    logger.error(future.cause()) { "[Upstream] Failed to send initial handshake response." }
                    proxyContext.downstream().closeOnFlush()
                    return@addListener
                }

                downstream.pipeline().addFirst(
                    "ssl-handler",
                    SslHandler(serverSslContext).apply {
                        handshakeFuture().addListener { future ->
                            if (!future.isSuccess) {
                                logger.error(future.cause()) { "[Downstream] SSL handshake failed." }
                                proxyContext.downstream().closeOnFlush()
                                return@addListener
                            }

                            logger.info { "[Downstream] SSL handshake completed successfully." }
                            proxyContext.upstream().pipeline().addFirst(
                                "ssl-handler",
                                SslHandler(clientSslContext).apply {
                                    handshakeFuture().addListener { future ->
                                        if (!future.isSuccess) {
                                            logger.error(future.cause()) { "[Upstream] SSL handshake failed." }
                                            proxyContext.downstream().closeOnFlush()
                                            return@addListener
                                        }

                                        logger.info { "[Upstream] SSL handshake completed successfully." }
                                    }
                                },
                            )
                            proxyContext.upstream().writeAndFlush(Unpooled.EMPTY_BUFFER) // Trigger the SSL handshake
                        }
                    },
                )
            }

            // If the server supports SSL, we should have already added the SslHandler

            return
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
        logger.trace { "Client Plugin Name: ${clientPluginName?.toString(Charsets.UTF_8)}" }

        val clientConnectAttrs =
            if (clientCapabilities.contains(CapabilityFlag.CLIENT_CONNECT_ATTRS)) {
                val lengthOfAllKeyValues = payload.readLenEncInteger()

                val keyValuesByteBuf = payload.readBytes(lengthOfAllKeyValues.toInt())

                val attrs = mutableListOf<Pair<String, String>>()
                while (keyValuesByteBuf.readableBytes() > 0) {
                    val key = keyValuesByteBuf.readLenEncString().toString(Charsets.UTF_8)
                    val value = keyValuesByteBuf.readLenEncString().toString(Charsets.UTF_8)
                    attrs.add(key to value)
                }
                ReferenceCountUtil.release(keyValuesByteBuf)
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

        payload.resetReaderIndex()

        proxyContext.upstream().apply {
            pipeline().addBefore(
                "relay-handler",
                "auth-switch-request-handler",
                AuthSwitchRequestHandler(proxyContext),
            )
            writeAndFlush(msg)
        }
        ctx.pipeline().remove(this)
    }

    override fun handlerAdded(ctx: ChannelHandlerContext) {
        logger.trace { this::class.simpleName + " added to pipeline" }
    }

    override fun handlerRemoved(ctx: ChannelHandlerContext) {
        logger.trace { this::class.simpleName + " removed from pipeline" }
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}
