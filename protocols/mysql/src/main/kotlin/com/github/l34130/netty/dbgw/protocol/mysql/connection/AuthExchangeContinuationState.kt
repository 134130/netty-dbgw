package com.github.l34130.netty.dbgw.protocol.mysql.connection

import com.github.l34130.netty.dbgw.core.MessageAction
import com.github.l34130.netty.dbgw.core.utils.netty.peek
import com.github.l34130.netty.dbgw.protocol.mysql.MySqlGatewayState
import com.github.l34130.netty.dbgw.protocol.mysql.Packet
import com.github.l34130.netty.dbgw.protocol.mysql.capabilities
import com.github.l34130.netty.dbgw.protocol.mysql.command.CommandPhaseState
import com.github.l34130.netty.dbgw.protocol.mysql.readRestOfPacketString
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.channel.ChannelHandlerContext

internal class AuthExchangeContinuationState : MySqlGatewayState() {
    override fun onDownstreamMessage(
        ctx: ChannelHandlerContext,
        msg: Packet,
    ): StateResult {
        val payload = msg.payload
        if (payload.readableBytes() < 1) {
            throw IllegalStateException("Received AuthSwitchResponse with no data")
        }

        val responseData = payload.readRestOfPacketString().toString(Charsets.UTF_8)
        logger.trace { "Received AuthSwitchResponse with data: $responseData" }

        return StateResult(
            nextState = AuthResultState(),
            action = MessageAction.Forward,
        )
    }

    override fun onUpstreamMessage(
        ctx: ChannelHandlerContext,
        msg: Packet,
    ): StateResult {
        val payload = msg.payload
        if (payload.peek { it.readUnsignedByte().toUInt() } == 0x01u) {
            // https://dev.mysql.com/doc/dev/mysql-server/latest/page_protocol_connection_phase_packets_protocol_auth_more_data.html
            // Extra authentication data beyond the initial challenge
            return StateResult(
                nextState = this,
                action = MessageAction.Forward,
            )
        }

        if (msg.isOkPacket()) {
            logger.trace { "Authentication succeeded" }
            return StateResult(
                nextState = CommandPhaseState(),
                action = MessageAction.Forward,
            )
        }

        if (msg.isErrorPacket()) {
            logger.trace {
                "Authentication failed: ${Packet.Error.readFrom(msg.payload, ctx.capabilities().enumSet())}"
            }
            return StateResult(
                nextState = this,
                action = MessageAction.Forward,
            )
        }

        throw IllegalStateException("Received unexpected packet type during authentication")
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}
