package com.github.l34130.netty.dbgw.app.handler.codec.mysql.connection

import com.github.l34130.netty.dbgw.app.handler.codec.mysql.GatewayState
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.Packet
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.capabilities
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.command.CommandPhaseState
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.downstream
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.readNullTerminatedString
import com.github.l34130.netty.dbgw.utils.netty.closeOnFlush
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.buffer.ByteBufUtil
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
            ctx.downstream().writeAndFlush(packet)
            TODO("Not yet implemented: handle AuthSwitchRequest packet")
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
            TODO("Not yet implemented: handle authentication error")
        }

        logger.warn {
            "Received unexpected packet type during authentication: ${ByteBufUtil.prettyHexDump(packet.payload)}"
        }

        // AuthMoreData packet will be here
        TODO("Not yet implemented: handle other types of packets during authentication")
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
