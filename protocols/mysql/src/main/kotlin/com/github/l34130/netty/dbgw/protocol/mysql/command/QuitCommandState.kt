package com.github.l34130.netty.dbgw.protocol.mysql.command

import com.github.l34130.netty.dbgw.core.downstream
import com.github.l34130.netty.dbgw.core.upstream
import com.github.l34130.netty.dbgw.core.utils.netty.closeOnFlush
import com.github.l34130.netty.dbgw.core.utils.netty.peek
import com.github.l34130.netty.dbgw.protocol.mysql.MySqlGatewayState
import com.github.l34130.netty.dbgw.protocol.mysql.Packet
import com.github.l34130.netty.dbgw.protocol.mysql.capabilities
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.channel.ChannelHandlerContext

internal class QuitCommandState : MySqlGatewayState {
    private var requested = false

    override fun onDownstreamPacket(
        ctx: ChannelHandlerContext,
        packet: Packet,
    ): MySqlGatewayState {
        check(!requested) { "Duplicate COM_QUIT request received." }
        requested = true
        ctx.upstream().writeAndFlush(packet)
        return this
    }

    override fun onUpstreamPacket(
        ctx: ChannelHandlerContext,
        packet: Packet,
    ): MySqlGatewayState {
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
