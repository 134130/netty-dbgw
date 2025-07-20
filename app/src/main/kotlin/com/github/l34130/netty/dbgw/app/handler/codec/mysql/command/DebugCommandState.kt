package com.github.l34130.netty.dbgw.app.handler.codec.mysql.command

import com.github.l34130.netty.dbgw.app.handler.codec.mysql.GatewayState
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.Packet
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.capabilities
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.downstream
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.upstream
import com.github.l34130.netty.dbgw.utils.netty.peek
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.channel.ChannelHandlerContext

class DebugCommandState : GatewayState {
    private var requested = false

    override fun onDownstreamPacket(
        ctx: ChannelHandlerContext,
        packet: Packet,
    ): GatewayState {
        check(!requested) { "Duplicate COM_DEBUG request received." }
        requested = true
        ctx.upstream().writeAndFlush(packet)
        return this
    }

    override fun onUpstreamPacket(
        ctx: ChannelHandlerContext,
        packet: Packet,
    ): GatewayState {
        check(requested) { "Received COM_DEBUG response without a prior request." }
        when {
            packet.isOkPacket() -> {
                logger.trace {
                    val okPacket = packet.payload.peek { Packet.Ok.readFrom(it, ctx.capabilities().enumSet()) }
                    "COM_DEBUG response: $okPacket"
                }
            }

            packet.isErrorPacket() -> {
                logger.trace {
                    val errorPacket = packet.payload.peek { Packet.Error.readFrom(it, ctx.capabilities().enumSet()) }
                    "COM_DEBUG response: $errorPacket"
                }
            }

            else -> logger.warn { "Unexpected COM_DEBUG response: $packet" }
        }

        ctx.downstream().writeAndFlush(packet)
        return CommandPhaseState()
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}
