package com.github.l34130.netty.dbgw.protocol.postgres

import com.github.l34130.netty.dbgw.core.MessageDirection
import com.github.l34130.netty.dbgw.core.MessageInterceptor
import io.netty.channel.ChannelHandlerContext

class TerminateInterceptor : MessageInterceptor {
    override fun intercept(
        ctx: ChannelHandlerContext,
        msg: Any,
        direction: MessageDirection,
    ): MessageInterceptor.InterceptResult {
        if (msg is Message && msg.type == 'X') {
            // Terminate message received, close the connection
            return MessageInterceptor.InterceptResult.Terminate(reason = "Terminate message received")
        }

        return MessageInterceptor.InterceptResult.Continue
    }
}
