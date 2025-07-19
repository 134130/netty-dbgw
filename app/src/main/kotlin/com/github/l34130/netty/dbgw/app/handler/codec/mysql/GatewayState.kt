package com.github.l34130.netty.dbgw.app.handler.codec.mysql

import io.netty.channel.ChannelHandlerContext

interface GatewayState {
    fun onDownstreamPacket(
        ctx: ChannelHandlerContext,
        packet: Packet,
    ): GatewayState =
        error(
            "Unexpected call to onDownstreamPacket in state '${this::class.simpleName}'. " +
                "This state does not handle downstream packets.",
        )

    fun onUpstreamPacket(
        ctx: ChannelHandlerContext,
        packet: Packet,
    ): GatewayState =
        error(
            "Unexpected call to onUpstreamPacket in state '${this::class.simpleName}'. " +
                "This state does not handle upstream packets.",
        )
}
