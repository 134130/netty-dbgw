package com.github.l34130.netty.dbgw.protocol.mysql.connection

import com.github.l34130.netty.dbgw.protocol.mysql.ClosingConnectionException
import com.github.l34130.netty.dbgw.protocol.mysql.GatewayState
import com.github.l34130.netty.dbgw.protocol.mysql.Packet
import com.github.l34130.netty.dbgw.protocol.mysql.capabilities
import com.github.l34130.netty.dbgw.protocol.mysql.command.CommandPhaseState
import com.github.l34130.netty.dbgw.protocol.mysql.downstream
import com.github.l34130.netty.dbgw.protocol.mysql.readNullTerminatedString
import com.github.l34130.netty.dbgw.utils.netty.closeOnFlush
import com.github.l34130.netty.dbgw.utils.netty.peek
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.channel.ChannelHandlerContext

class AuthResultState : GatewayState {
    override fun onUpstreamPacket(
        ctx: ChannelHandlerContext,
        packet: Packet,
    ): GatewayState {
        if (packet.isEofPacket()) {
            packet.payload.markReaderIndex()
            packet.payload.skipBytes(1) // Skip the first byte (EOF marker)

            logger.trace {
                val pluginName = packet.payload.readNullTerminatedString().toString(Charsets.US_ASCII)
                "Received AuthSwitchRequest with plugin: $pluginName"
            }

            packet.payload.resetReaderIndex()
            return AuthSwitchState().onUpstreamPacket(ctx, packet)
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
            ctx.downstream().closeOnFlush()
            throw ClosingConnectionException("Authentication failed")
        }

        if (packet.payload.peek { it.readUnsignedByte().toUInt() } == 0x1u) {
            // AuthMoreData packet
            // https://dev.mysql.com/doc/dev/mysql-server/latest/page_protocol_connection_phase_packets_protocol_auth_more_data.html
            return AuthExchangeContinuationState().onUpstreamPacket(ctx, packet)
        }

        error("Unexpected packet type during authentication: ${packet.payload.peek { it.readUnsignedByte().toUInt() }}")
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
