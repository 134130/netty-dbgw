
package com.github.l34130.netty.dbgw.core

import io.netty.channel.ChannelHandlerContext

abstract class GatewayState<F : Any, B : Any> {
    data class StateResult(
        val nextState: GatewayState<*, *>,
        val action: MessageAction,
    )

    open fun onFrontendMessage(
        ctx: ChannelHandlerContext,
        msg: F,
    ): StateResult = error("onFrontendMessage() not implemented for ${this::class.java.simpleName}")

    open fun onBackendMessage(
        ctx: ChannelHandlerContext,
        msg: B,
    ): StateResult = error("onBackendMessage() not implemented for ${this::class.java.simpleName}")
}

abstract class BidirectionalGatewayState<T : Any> : GatewayState<T, T>()
