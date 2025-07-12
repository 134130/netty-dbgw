package com.github.l34130.netty.dbgw.app.handler.codec.mysql

import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.ssl.SslHandler
import javax.net.ssl.SSLContext
import kotlin.math.max

// https://dev.mysql.com/doc/dev/mysql-server/latest/page_protocol_connection_phase_packets_protocol_handshake_v10.html
class InitialHandshakeRequestInboundHandler(
    private val proxyContext: ProxyContext,
) : SimpleChannelInboundHandler<Packet>() {
    override fun channelRead0(
        ctx: ChannelHandlerContext,
        msg: Packet,
    ) {
        val payload = msg.payload
        val protocolVersion = payload.readFixedLengthInteger(1)
        if (protocolVersion.value != 10L) {
            logger.error { "Unsupported MySQL protocol version: ${protocolVersion.value}" }
            ctx.close()
            return
        }

        // human-readable server version
        val serverVersion = payload.readNullTerminatedString()
        logger.debug { "Server version: $serverVersion" }

        // connection id
        val threadId = payload.readFixedLengthInteger(4)
        logger.debug { "Thread ID: ${threadId.value}" }

        // scramble buffer
        val authPluginDataPart1 = payload.readBytes(8)
        payload.skipBytes(1) // skip filler byte

        val capabilityFlags1 = payload.readFixedLengthInteger(2)
        val characterSet = payload.readFixedLengthInteger(1)
        val statusFlags = payload.readFixedLengthInteger(2)
        val capabilityFlags2 = payload.readFixedLengthInteger(2)
        val serverCapabilities = CapabilitiesFlags(capabilityFlags1.value, capabilityFlags2.value)
        proxyContext.serverCapabilities = serverCapabilities

        val supportsClientPluginAuth = (serverCapabilities.hasFlag(CapabilitiesFlags.CLIENT_PLUGIN_AUTH))
        val authPluginDataLength =
            if (supportsClientPluginAuth) {
                payload.readFixedLengthInteger(1).value.toInt()
            } else {
                0x00
            }
        logger.debug { "Supports Client Plugin Auth: $supportsClientPluginAuth" }

        payload.skipBytes(10) // skip reserved bytes

        val authPluginDataPart2 = payload.readBytes(max(13, authPluginDataLength - 8))
        if (supportsClientPluginAuth) {
            val authPluginName = payload.readNullTerminatedString()
            logger.debug { "Auth Plugin Name: $authPluginName" }
        }

        ctx.pipeline().remove(this)
        ctx.fireChannelRead(msg)
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}

class InitialHandshakeResponseInboundHandler(
    private val proxyContext: ProxyContext,
) : SimpleChannelInboundHandler<Packet>() {
    override fun channelRead0(
        ctx: ChannelHandlerContext,
        msg: Packet,
    ) {
        val payload = msg.payload
        val clientFlag = payload.readFixedLengthInteger(4)
        if ((clientFlag.value and CapabilitiesFlags.CLIENT_PROTOCOL_41) == 0L) {
            logger.error { "Unsupported MySQL client protocol version: ${clientFlag.value}" }
            ctx.close()
            return
        }

        val maxPacketSize = payload.readFixedLengthInteger(4)
        val characterSet = payload.readFixedLengthInteger(1)
        payload.skipBytes(23) // skip filler bytes

        if (proxyContext.serverCapabilities.hasFlag(CapabilitiesFlags.CLIENT_SSL)) {
            // If the server supports SSL, we should have already added the SslHandler
            ctx.pipeline().addAfter(
                "ssl-connection-request-handler",
                "ssl-handler",
                SslHandler(SSLContext.getDefault().createSSLEngine()),
            )
        }

        // login username
        val username = payload.readNullTerminatedString()
        logger.debug { "Username: $username" }

        val authResponse =
            if ((clientFlag.value and CapabilitiesFlags.CLIENT_PLUGIN_AUTH_LENENC_CLIENT_DATA) != 0L) {
                payload.readLengthEncodedString()
            } else {
                val authResponseLength = payload.readFixedLengthInteger(1).value.toInt()
                payload.readBytes(authResponseLength)
            }

        val database =
            if (clientFlag.value and CapabilitiesFlags.CLIENT_CONNECT_WITH_DB != 0L) {
                payload.readNullTerminatedString()
            } else {
                null
            }
        logger.debug { "Database: $database" }

        val clientPluginName =
            if ((clientFlag.value and CapabilitiesFlags.CLIENT_PLUGIN_AUTH) != 0L) {
                payload.readNullTerminatedString()
            } else {
                null
            }
        logger.debug { "Client Plugin Name: $clientPluginName" }

        val clientConnectAttrs =
            if ((clientFlag.value and CapabilitiesFlags.CLIENT_CONNECT_ATTRS) != 0L) {
                val lengthOfAllKeyValues = payload.readLengthEncodedInteger()

                val attrs = mutableListOf<Pair<String, String>>()
                (0 until lengthOfAllKeyValues).forEach { i ->
                    val key = payload.readLengthEncodedString().toString(Charsets.UTF_8)
                    val value = payload.readLengthEncodedString().toString(Charsets.UTF_8)
                    attrs.add(key to value)
                }
            } else {
                emptyList<Pair<String, String>>()
            }
        logger.debug { "Client Connect Attributes: $clientConnectAttrs" }

        val zstdCompressionLevel =
            if ((clientFlag.value and CapabilitiesFlags.CLIENT_ZSTD_COMPRESSION_ALGORITHM) != 0L) {
                payload.readFixedLengthInteger(1).value.toInt()
            } else {
                0
            }

        ctx.pipeline().remove(this)
        ctx.fireChannelRead(msg)
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}
