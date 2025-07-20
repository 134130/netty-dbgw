package com.github.l34130.netty.dbgw.protocol.mysql

import com.github.l34130.netty.dbgw.core.GatewayState
import io.netty.channel.ChannelHandlerContext

internal interface MySqlGatewayState : GatewayState<Packet> {
    override fun onDownstreamMessage(
        ctx: ChannelHandlerContext,
        msg: Packet,
    ): GatewayState<Packet> = onDownstreamPacket(ctx, msg)

    override fun onUpstreamMessage(
        ctx: ChannelHandlerContext,
        msg: Packet,
    ): GatewayState<Packet> = onUpstreamPacket(ctx, msg)

    @Deprecated(
        message = "Use onDownstreamMessage instead",
        replaceWith = ReplaceWith("onDownstreamPacket(ctx, packet)"),
    )
    fun onDownstreamPacket(
        ctx: ChannelHandlerContext,
        packet: Packet,
    ): MySqlGatewayState =
        error(
            buildString {
                append("Unexpected call to onDownstreamPacket in state '${this::class.simpleName}'. ")
                append("This state does not handle downstream packets.")
            },
        )

    @Deprecated(
        message = "Use onUpstreamMessage instead",
        replaceWith = ReplaceWith("onUpstreamMessage(ctx, packet)"),
    )
    fun onUpstreamPacket(
        ctx: ChannelHandlerContext,
        packet: Packet,
    ): MySqlGatewayState =
        error(
            buildString {
                append("Unexpected call to onUpstreamPacket in state '${this::class.simpleName}'. ")
                append("This state does not handle upstream packets.")
            },
        )
}
