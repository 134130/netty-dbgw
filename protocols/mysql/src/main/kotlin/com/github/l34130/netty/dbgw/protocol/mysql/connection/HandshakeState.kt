package com.github.l34130.netty.dbgw.protocol.mysql.connection

import com.github.l34130.netty.dbgw.core.MessageAction
import com.github.l34130.netty.dbgw.core.utils.toEnumSet
import com.github.l34130.netty.dbgw.protocol.mysql.MySqlGatewayState
import com.github.l34130.netty.dbgw.protocol.mysql.Packet
import com.github.l34130.netty.dbgw.protocol.mysql.capabilities
import com.github.l34130.netty.dbgw.protocol.mysql.constant.CapabilityFlag
import com.github.l34130.netty.dbgw.protocol.mysql.readFixedLengthInteger
import com.github.l34130.netty.dbgw.protocol.mysql.readNullTerminatedString
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.channel.ChannelHandlerContext
import java.util.EnumSet
import kotlin.math.max

// https://dev.mysql.com/doc/dev/mysql-server/latest/page_protocol_connection_phase_packets_protocol_handshake.html
internal class HandshakeState : MySqlGatewayState() {
    override fun onBackendMessage(
        ctx: ChannelHandlerContext,
        msg: Packet,
    ): StateResult {
        val payload = msg.payload
        val protocolVersion = payload.readFixedLengthInteger(1)
        if (protocolVersion != 10UL) {
            logger.error { "Unsupported MySQL protocol version: $protocolVersion" }
            ctx.close()
            TODO("Handle unsupported protocol version")
        }

        // human-readable server version
        val serverVersion = payload.readNullTerminatedString()
        logger.trace { "Server version: ${serverVersion.toString(Charsets.UTF_8)}" }

        // connection id
        val threadId = payload.readFixedLengthInteger(4)
        logger.trace { "Thread ID: $threadId" }

        // scramble buffer
        val authPluginDataPart1 = payload.readSlice(8)
        payload.skipBytes(1) // skip filler byte

        val capabilityFlags1 = payload.readFixedLengthInteger(2)
        val characterSet = payload.readFixedLengthInteger(1)
        val statusFlags = payload.readFixedLengthInteger(2)
        val capabilityFlags2 = payload.readFixedLengthInteger(2)
        val serverCapabilities: EnumSet<CapabilityFlag> = ((capabilityFlags1) or (capabilityFlags2 shl 16)).toEnumSet()
        ctx.capabilities().setServerCapabilities(serverCapabilities)
        logger.trace { "Server Capabilities: $serverCapabilities" }

        val supportsClientPluginAuth = (serverCapabilities.contains(CapabilityFlag.CLIENT_PLUGIN_AUTH))
        val authPluginDataLength =
            if (supportsClientPluginAuth) {
                payload.readFixedLengthInteger(1).toInt()
            } else {
                0x00
            }

        payload.skipBytes(10) // skip reserved bytes

        val authPluginDataPart2 = payload.readSlice(max(13, authPluginDataLength - 8))
        if (supportsClientPluginAuth) {
            val authPluginName = payload.readNullTerminatedString()
            logger.trace { "Server Auth Plugin Name: ${authPluginName.toString(Charsets.UTF_8)}" }
        }

        return StateResult(
            nextState = HandshakeResponseState(),
            action = MessageAction.Forward,
        )
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
