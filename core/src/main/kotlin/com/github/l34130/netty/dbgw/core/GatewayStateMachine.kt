package com.github.l34130.netty.dbgw.core

import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.channel.ChannelHandlerContext

abstract class GatewayStateMachine<T>(
    initialState: GatewayState<T>,
) {
    private val logger = KotlinLogging.logger(javaClass.canonicalName)
    private var currentState: GatewayState<T> = initialState

    fun processDownstreamMessage(
        ctx: ChannelHandlerContext,
        msg: T,
    ) {
        logger.debug { "[DOWNSTREAM] ${currentState::class.simpleName}" }
        val nextState = currentState.onDownstreamMessage(ctx, msg)
        logger.debug { "[DOWNSTREAM] ${currentState::class.simpleName} -> ${nextState::class.simpleName}" }
        currentState = nextState
    }

    fun processUpstreamMessage(
        ctx: ChannelHandlerContext,
        msg: T,
    ) {
        logger.debug { "[ UPSTREAM ] ${currentState::class.simpleName}" }
        val nextState = currentState.onUpstreamMessage(ctx, msg)
        logger.debug { "[DOWNSTREAM] ${currentState::class.simpleName} -> ${nextState::class.simpleName}" }
        currentState = nextState
    }
}
