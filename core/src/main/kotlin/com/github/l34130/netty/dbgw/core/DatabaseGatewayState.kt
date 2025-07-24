
package com.github.l34130.netty.dbgw.core

import io.netty.channel.ChannelHandlerContext

abstract class DatabaseGatewayState<D : Any, U : Any> {
    data class StateResult(
        val nextState: DatabaseGatewayState<*, *>,
        val action: MessageAction,
    )

    open fun onDownstreamMessage(
        ctx: ChannelHandlerContext,
        msg: D,
    ): StateResult = error("onDownstreamMessage() not implemented for ${this::class.java.simpleName}")

    open fun onUpstreamMessage(
        ctx: ChannelHandlerContext,
        msg: U,
    ): StateResult = error("onUpstreamMessage() not implemented for ${this::class.java.simpleName}")
}

abstract class BidirectionalDatabaseGatewayState<T : Any> : DatabaseGatewayState<T, T>()
