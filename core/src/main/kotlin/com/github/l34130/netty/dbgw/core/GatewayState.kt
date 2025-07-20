package com.github.l34130.netty.dbgw.core

import io.netty.channel.ChannelHandlerContext

interface GatewayState<T> {
    fun onDownstreamMessage(
        ctx: ChannelHandlerContext,
        msg: T,
    ): GatewayState<T> =
        error(
            buildString {
                append("Unexpected call to onDownstreamMessage in state '${this::class.simpleName}'. ")
                append("This state does not handle downstream messages.")
            },
        )

    fun onUpstreamMessage(
        ctx: ChannelHandlerContext,
        msg: T,
    ): GatewayState<T> {
        error(
            buildString {
                append("Unexpected call to onUpstreamMessage in state '${this::class.simpleName}'. ")
                append("This state does not handle upstream messages.")
            },
        )
    }
}
