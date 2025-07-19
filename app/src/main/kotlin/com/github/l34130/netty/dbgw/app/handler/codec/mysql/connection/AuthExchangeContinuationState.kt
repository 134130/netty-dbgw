package com.github.l34130.netty.dbgw.app.handler.codec.mysql.connection

import com.github.l34130.netty.dbgw.app.handler.codec.mysql.GatewayState
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.Packet
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.capabilities
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.command.CommandPhaseState
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.downstream
import com.github.l34130.netty.dbgw.utils.netty.peek
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.channel.ChannelHandlerContext

class AuthExchangeContinuationState : GatewayState {
    override fun onUpstreamPacket(
        ctx: ChannelHandlerContext,
        packet: Packet,
    ): GatewayState {
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
