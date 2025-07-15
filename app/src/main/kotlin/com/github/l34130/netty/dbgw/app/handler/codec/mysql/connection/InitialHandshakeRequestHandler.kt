package com.github.l34130.netty.dbgw.app.handler.codec.mysql.connection

import com.github.l34130.netty.dbgw.app.handler.codec.mysql.Packet
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.ProxyContext
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.constant.CapabilityFlag
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.readFixedLengthInteger
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.readNullTerminatedString
import com.github.l34130.netty.dbgw.utils.toEnumSet
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import java.util.EnumSet
import kotlin.math.max

// https://dev.mysql.com/doc/dev/mysql-server/latest/page_protocol_connection_phase_packets_protocol_handshake_v10.html
class InitialHandshakeRequestHandler(
    private val proxyContext: ProxyContext,
) : SimpleChannelInboundHandler<Packet>() {
    override fun channelRead0(
        ctx: ChannelHandlerContext,
        msg: Packet,
    ) {
        val payload = msg.payload
        payload.markReaderIndex()

        val protocolVersion = payload.readFixedLengthInteger(1)
        if (protocolVersion != 10UL) {
            logger.error { "Unsupported MySQL protocol version: $protocolVersion" }
            ctx.close()
            return
        }

        // human-readable server version
        val serverVersion = payload.readNullTerminatedString()
        logger.trace { "Server version: ${serverVersion.toString(Charsets.UTF_8)}" }

        // connection id
        val threadId = payload.readFixedLengthInteger(4)
        logger.trace { "Thread ID: $threadId" }

        // scramble buffer
        val authPluginDataPart1 = payload.readBytes(8)
        payload.skipBytes(1) // skip filler byte

        val capabilityFlags1 = payload.readFixedLengthInteger(2)
        val characterSet = payload.readFixedLengthInteger(1)
        val statusFlags = payload.readFixedLengthInteger(2)
        val capabilityFlags2 = payload.readFixedLengthInteger(2)
        val serverCapabilities: EnumSet<CapabilityFlag> = ((capabilityFlags1) or (capabilityFlags2 shl 32)).toInt().toEnumSet()
        proxyContext.setServerCapabilities(serverCapabilities)

        val supportsClientPluginAuth = (serverCapabilities.contains(CapabilityFlag.CLIENT_PLUGIN_AUTH))
        val authPluginDataLength =
            if (supportsClientPluginAuth) {
                payload.readFixedLengthInteger(1).toInt()
            } else {
                0x00
            }
        logger.trace { "Supports Client Plugin Auth: $supportsClientPluginAuth" }

        payload.skipBytes(10) // skip reserved bytes

        val authPluginDataPart2 = payload.readBytes(max(13, authPluginDataLength - 8))
        if (supportsClientPluginAuth) {
            val authPluginName = payload.readNullTerminatedString()
            logger.trace { "Auth Plugin Name: ${authPluginName.toString(Charsets.UTF_8)}" }
        }

        payload.resetReaderIndex()
        proxyContext.downstream().writeAndFlush(msg)
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
