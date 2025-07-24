package com.github.l34130.netty.dbgw.core

import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.channel.ChannelHandlerContext
import io.netty.util.internal.TypeParameterMatcher

class DatabaseStateMachine(
    initialState: DatabaseGatewayState<*, *>,
    private val interceptors: List<MessageInterceptor> = emptyList(),
) {
    private var state: DatabaseGatewayState<*, *> = initialState
    private val logger = KotlinLogging.logger { }

    fun processDownstreamMessage(
        ctx: ChannelHandlerContext,
        msg: Any,
    ): MessageAction {
        val interceptorResult = executeInterceptors(ctx, msg, MessageDirection.DOWNSTREAM)
        return when (interceptorResult) {
            is MessageInterceptor.InterceptResult.Complete -> {
                logger.debug {
                    "Intercepted message in DOWNSTREAM direction: ${msg::class.java.simpleName}, action: ${interceptorResult.action}"
                }
                state = interceptorResult.nextState
                interceptorResult.action
            }
            is MessageInterceptor.InterceptResult.Terminate -> {
                MessageAction.Terminate(interceptorResult.reason)
            }
            MessageInterceptor.InterceptResult.Continue -> processMessage(ctx, msg, MessageDirection.DOWNSTREAM)
        }
    }

    fun processUpstreamMessage(
        ctx: ChannelHandlerContext,
        msg: Any,
    ): MessageAction {
        val interceptorResult = executeInterceptors(ctx, msg, MessageDirection.UPSTREAM)
        return when (interceptorResult) {
            is MessageInterceptor.InterceptResult.Complete -> {
                logger.debug {
                    "Intercepted message in UPSTREAM direction: ${msg::class.java.simpleName}, action: ${interceptorResult.action}"
                }
                state = interceptorResult.nextState
                interceptorResult.action
            }
            is MessageInterceptor.InterceptResult.Terminate -> {
                MessageAction.Terminate(interceptorResult.reason)
            }
            MessageInterceptor.InterceptResult.Continue -> processMessage(ctx, msg, MessageDirection.UPSTREAM)
        }
    }

    private fun processMessage(
        ctx: ChannelHandlerContext,
        msg: Any,
        direction: MessageDirection,
    ): MessageAction {
        val matcher =
            when (direction) {
                MessageDirection.DOWNSTREAM -> TypeParameterMatcher.find(state, DatabaseGatewayState::class.java, "D")
                MessageDirection.UPSTREAM -> TypeParameterMatcher.find(state, DatabaseGatewayState::class.java, "U")
            }
        check(matcher.match(msg)) {
            "Message type '${msg::class.java.simpleName}' does not match expected type for state '${state::class.java.simpleName}'"
        }

        @Suppress("UNCHECKED_CAST")
        val state = state as DatabaseGatewayState<Any, Any>
        logger.trace { "[$direction] Processing message in state: ${state::class.java.simpleName}" }

        val result =
            when (direction) {
                MessageDirection.DOWNSTREAM -> state.onDownstreamMessage(ctx, msg)
                MessageDirection.UPSTREAM -> state.onUpstreamMessage(ctx, msg)
            }

        logger.trace { "[$direction] Resulting action: ${result.action}, next state: ${result.nextState::class.java.simpleName}" }

        this.state = result.nextState
        return result.action
    }

    private fun executeInterceptors(
        ctx: ChannelHandlerContext,
        msg: Any,
        direction: MessageDirection,
    ): MessageInterceptor.InterceptResult {
        for (interceptor in interceptors) {
            val result = interceptor.intercept(ctx, msg, direction)
            if (result !is MessageInterceptor.InterceptResult.Continue) {
                return result
            }
        }
        return MessageInterceptor.InterceptResult.Continue
    }
}
