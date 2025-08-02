package com.github.l34130.netty.dbgw.core

import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.timeout.IdleState
import io.netty.handler.timeout.IdleStateEvent
import com.github.l34130.netty.dbgw.core.utils.netty.closeOnFlush

class IdleTimeoutHandler : ChannelDuplexHandler() {
    override fun userEventTriggered(ctx: ChannelHandlerContext, evt: Any) {
        if (evt is IdleStateEvent) {
            if (evt.state() == IdleState.ALL_IDLE) {
                logger.info { "Closing idle connection: ${ctx.channel().remoteAddress()}" }
                ctx.channel().closeOnFlush()
            }
        }
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}
