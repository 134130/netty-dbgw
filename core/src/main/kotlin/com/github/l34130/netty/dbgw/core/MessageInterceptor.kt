package com.github.l34130.netty.dbgw.core

import io.netty.channel.ChannelHandlerContext

fun interface MessageInterceptor {
    fun intercept(
        ctx: ChannelHandlerContext,
        msg: Any,
        direction: MessageDirection,
    ): InterceptResult

    sealed class InterceptResult {
        object Continue : InterceptResult()

        data class Complete(
            val action: MessageAction,
            val nextState: GatewayState<*, *>,
        ) : InterceptResult()

        data class Terminate(
            val reason: String? = null,
        ) : InterceptResult()
    }
}
