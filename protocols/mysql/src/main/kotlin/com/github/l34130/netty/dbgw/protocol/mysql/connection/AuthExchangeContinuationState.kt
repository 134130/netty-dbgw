package com.github.l34130.netty.dbgw.protocol.mysql.connection

import com.github.l34130.netty.dbgw.core.downstream
import com.github.l34130.netty.dbgw.core.utils.netty.peek
import com.github.l34130.netty.dbgw.protocol.mysql.MySqlGatewayState
import com.github.l34130.netty.dbgw.protocol.mysql.Packet
import com.github.l34130.netty.dbgw.protocol.mysql.capabilities
import com.github.l34130.netty.dbgw.protocol.mysql.command.CommandPhaseState
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.channel.ChannelHandlerContext

internal class AuthExchangeContinuationState : MySqlGatewayState {
    override fun onUpstreamPacket(
        ctx: ChannelHandlerContext,
        packet: Packet,
    ): MySqlGatewayState {
        val payload = packet.payload

        if (payload.peek { it.readUnsignedByte().toUInt() } == 0x01u) {
            // https://dev.mysql.com/doc/dev/mysql-server/latest/page_protocol_connection_phase_packets_protocol_auth_more_data.html
            // Extra authentication data beyond the initial challenge
            ctx.downstream().writeAndFlush(packet)
            return this
        }

        if (packet.isOkPacket()) {
            logger.trace { "Authentication succeeded" }
            ctx.downstream().writeAndFlush(packet)
            return CommandPhaseState()
        }

        if (packet.isErrorPacket()) {
            packet.payload.markReaderIndex()
            logger.trace {
                "Authentication failed: ${Packet.Error.readFrom(packet.payload, ctx.capabilities().enumSet())}"
            }
            packet.payload.resetReaderIndex()
            ctx.downstream().writeAndFlush(packet)
            TODO("Handle authentication error")
        }

        throw IllegalStateException("Received unexpected packet type during authentication")
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}
