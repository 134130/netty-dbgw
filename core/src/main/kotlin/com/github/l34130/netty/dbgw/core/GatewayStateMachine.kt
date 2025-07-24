package com.github.l34130.netty.dbgw.core

import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.channel.ChannelHandlerContext

@Deprecated("Use DatabaseGatewayState instead")
abstract class GatewayStateMachine<T, S : GatewayState<T>>(
    initialState: S,
) {
    private val logger = KotlinLogging.logger(javaClass.canonicalName)
    private var currentState: S = initialState

    fun processDownstreamMessage(
        ctx: ChannelHandlerContext,
        msg: T,
    ) {
        logger.debug { "[DOWNSTREAM] ${currentState::class.simpleName}" }
        val nextState = currentState.onDownstreamMessage(ctx, msg)
        logger.debug { "[DOWNSTREAM] ${currentState::class.simpleName} -> ${nextState::class.simpleName}" }
        @Suppress("UNCHECKED_CAST")
        currentState = nextState as S
    }

    fun processUpstreamMessage(
        ctx: ChannelHandlerContext,
        msg: T,
    ) {
        logger.debug { "[ UPSTREAM ] ${currentState::class.simpleName}" }
        val nextState = currentState.onUpstreamMessage(ctx, msg)
        logger.debug { "[DOWNSTREAM] ${currentState::class.simpleName} -> ${nextState::class.simpleName}" }
        @Suppress("UNCHECKED_CAST")
        currentState = nextState as S
    }
}
