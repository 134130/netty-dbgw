package com.github.l34130.netty.dbgw.protocol.mysql.connection

import com.github.l34130.netty.dbgw.core.MessageAction
import com.github.l34130.netty.dbgw.core.utils.netty.peek
import com.github.l34130.netty.dbgw.protocol.mysql.MySqlGatewayState
import com.github.l34130.netty.dbgw.protocol.mysql.Packet
import com.github.l34130.netty.dbgw.protocol.mysql.capabilities
import com.github.l34130.netty.dbgw.protocol.mysql.command.CommandPhaseState
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.channel.ChannelHandlerContext

internal class AuthResultState(
    private val pluginName: String,
    private val password: String?,
) : MySqlGatewayState() {
    override fun onBackendMessage(
        ctx: ChannelHandlerContext,
        msg: Packet,
    ): StateResult {
        if (msg.isEofPacket()) {
            return AuthSwitchState(password).onBackendMessage(ctx, msg)
        }

        if (msg.isOkPacket()) {
            logger.trace { "Authentication succeeded" }
            return StateResult(
                nextState = CommandPhaseState(),
                action = MessageAction.Forward,
            )
        }

        if (msg.isErrorPacket()) {
            val error = Packet.Error.readFrom(msg.payload, ctx.capabilities().enumSet())
            logger.trace { "Authentication failed: $error" }
            return StateResult(
                nextState = this,
                action = MessageAction.Forward,
            )
        }

        if (msg.payload.peek { it.readUnsignedByte().toUInt() } == 0x1u) {
            // AuthMoreData packet
            // https://dev.mysql.com/doc/dev/mysql-server/latest/page_protocol_connection_phase_packets_protocol_auth_more_data.html
            return AuthExchangeContinuationState(pluginName, password).onBackendMessage(ctx, msg)
        }

        error("Unexpected packet type during authentication: ${msg.payload.peek { it.readUnsignedByte().toUInt() }}")
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
