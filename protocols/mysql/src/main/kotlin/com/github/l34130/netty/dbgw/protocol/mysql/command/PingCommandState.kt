package com.github.l34130.netty.dbgw.protocol.mysql.command

import com.github.l34130.netty.dbgw.protocol.mysql.GatewayState
import com.github.l34130.netty.dbgw.protocol.mysql.Packet
import com.github.l34130.netty.dbgw.protocol.mysql.downstream
import com.github.l34130.netty.dbgw.protocol.mysql.upstream
import io.netty.channel.ChannelHandlerContext

class PingCommandState : GatewayState {
    private var requested = false

    override fun onDownstreamPacket(
        ctx: ChannelHandlerContext,
        packet: Packet,
    ): GatewayState {
        check(!requested) { "Duplicate COM_PING request received." }
        requested = true
        ctx.upstream().writeAndFlush(packet)
        return this
    }

    override fun onUpstreamPacket(
        ctx: ChannelHandlerContext,
        packet: Packet,
    ): GatewayState {
        check(requested) { "Received COM_PING response without a prior request." }
        check(packet.isOkPacket()) { "Expected OK packet for COM_PING, but got: $packet" }

        ctx.downstream().writeAndFlush(packet)
        return CommandPhaseState()
    }
}
