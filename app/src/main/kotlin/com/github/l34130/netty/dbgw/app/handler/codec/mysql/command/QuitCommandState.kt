package com.github.l34130.netty.dbgw.app.handler.codec.mysql.command

import com.github.l34130.netty.dbgw.app.handler.codec.mysql.GatewayState
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.Packet
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.capabilities
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.downstream
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.upstream
import com.github.l34130.netty.dbgw.utils.netty.closeOnFlush
import com.github.l34130.netty.dbgw.utils.netty.peek
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.channel.ChannelHandlerContext

class QuitCommandState : GatewayState {
    private var requested = false

    override fun onDownstreamPacket(
        ctx: ChannelHandlerContext,
        packet: Packet,
    ): GatewayState {
        check(!requested) { "Duplicate COM_QUIT request received." }
        requested = true
        ctx.upstream().writeAndFlush(packet)
        return this
    }

    override fun onUpstreamPacket(
        ctx: ChannelHandlerContext,
        packet: Packet,
    ): GatewayState {
        check(requested) { "Received COM_QUIT response without a prior request." }
        check(packet.isErrorPacket()) { "Expected an error packet for COM_QUIT, but got: $packet" }

        logger.trace {
            val errPacket = packet.payload.peek { Packet.Error.readFrom(it, ctx.capabilities().enumSet()) }
            "COM_QUIT response: $errPacket"
        }

        ctx.downstream().writeAndFlush(packet)
        ctx.downstream().closeOnFlush()
        return CommandPhaseState()
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}
