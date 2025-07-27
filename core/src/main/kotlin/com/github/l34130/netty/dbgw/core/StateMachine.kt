package com.github.l34130.netty.dbgw.core

import com.github.l34130.netty.dbgw.core.utils.concurrent.ChannelBasedEventExecutorChooser
import com.github.l34130.netty.dbgw.core.utils.concurrent.DefaultThreadFactory
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.channel.ChannelHandlerContext
import io.netty.util.NettyRuntime
import io.netty.util.concurrent.DefaultEventExecutorGroup
import io.netty.util.concurrent.EventExecutorGroup
import io.netty.util.concurrent.Promise
import io.netty.util.internal.TypeParameterMatcher

class StateMachine(
    initialState: GatewayState<*, *>,
    private val interceptors: List<MessageInterceptor> = emptyList(),
) {
    private var state: GatewayState<*, *> = initialState
    private val logger = KotlinLogging.logger { }

    private val businessEventExecutorChooser =
        ChannelBasedEventExecutorChooser(
            executors =
                DefaultEventExecutorGroup(
                    NettyRuntime.availableProcessors(),
                    DefaultThreadFactory("businessEventExecutorGroup"),
                ).toList().toTypedArray(),
        )
    private val isInterceptorsBusinessLogicAware: Boolean = interceptors.any { it is BusinessLogicAware }
    private val isBusinessLogicAware
        get() = isInterceptorsBusinessLogicAware || state is BusinessLogicAware

    fun processFrontendMessage(
        ctx: ChannelHandlerContext,
        msg: Any,
    ): Promise<MessageAction> {
    return processMessage(ctx, msg, MessageDirection.FRONTEND) {
        val interceptorResult = executeInterceptors(ctx, msg, MessageDirection.FRONTEND)
        when (interceptorResult) {
            is MessageInterceptor.InterceptResult.Complete -> {
                logger.debug {
                    "Intercepted message in FRONTEND direction: ${msg::class.java.simpleName}, action: ${interceptorResult.action}"
                }
                state = interceptorResult.nextState
                interceptorResult.action
            }

            is MessageInterceptor.InterceptResult.Terminate -> {
                MessageAction.Terminate(interceptorResult.reason)
            }

            MessageInterceptor.InterceptResult.Continue -> processMessageInCurrentState(
                ctx,
                msg,
                MessageDirection.FRONTEND
            )
        }
        }
    }

    fun processBackendMessage(
        ctx: ChannelHandlerContext,
        msg: Any,
    ): Promise<MessageAction> {
    return processMessage(ctx, msg, MessageDirection.BACKEND) {
        val interceptorResult = executeInterceptors(ctx, msg, MessageDirection.BACKEND)
        when (interceptorResult) {
            is MessageInterceptor.InterceptResult.Complete -> {
                logger.debug {
                    "Intercepted message in BACKEND direction: ${msg::class.java.simpleName}, action: ${interceptorResult.action}"
                }
                state = interceptorResult.nextState
                interceptorResult.action
            }

            is MessageInterceptor.InterceptResult.Terminate -> {
                MessageAction.Terminate(interceptorResult.reason)
                }

            MessageInterceptor.InterceptResult.Continue -> processMessageInCurrentState(
                ctx,
                msg,
                MessageDirection.BACKEND
            )
            }
        }
    }

private fun processMessageInCurrentState(
        ctx: ChannelHandlerContext,
        msg: Any,
    direction: MessageDirection,
    processor: () -> MessageAction,
): Promise<MessageAction> {
    val promise: Promise<MessageAction> = ctx.executor().newPromise()
    val executor = if (isBusinessLogicAware) businessEventExecutorChooser.choose(ctx.channel()) else ctx.executor()

    executor.submit {
        val result = runCatching { processor() }
        promise.setResultSafely(ctx, executor, result)
        }

    return promise
    }

    private fun processMessage(
        ctx: ChannelHandlerContext,
        msg: Any,
        direction: MessageDirection,
    ): MessageAction {
        val matcher =
            when (direction) {
                MessageDirection.FRONTEND -> TypeParameterMatcher.find(state, GatewayState::class.java, "F")
                MessageDirection.BACKEND -> TypeParameterMatcher.find(state, GatewayState::class.java, "B")
            }
    if (!matcher.match(msg)) {
        throw IllegalStateException(
            "Message type '${msg::class.java.simpleName}' does not match expected type for state '${state::class.java.simpleName}'"
        )
        }

        @Suppress("UNCHECKED_CAST")
        val state = state as GatewayState<Any, Any>
        logger.trace { "[$direction] Processing message in state: ${state::class.java.simpleName}" }

        val result =
            when (direction) {
                MessageDirection.FRONTEND -> state.onFrontendMessage(ctx, msg)
                MessageDirection.BACKEND -> state.onBackendMessage(ctx, msg)
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

    private fun <T> Promise<T>.setResultSafely(
        ctx: ChannelHandlerContext,
        executor: EventExecutorGroup,
        result: Result<T>,
    ) {
        if (executor == ctx.executor()) {
            // If the executor is the same as the context's executor, we can set the result directly
            result.fold(
                onSuccess = { setSuccess(it) },
                onFailure = { setFailure(it) },
            )
        } else {
            // If the executor is different, we need to schedule the result setting on the context's executor
            // to ensure thread safety
            ctx.executor().submit {
                result.fold(
                    onSuccess = { setSuccess(it) },
                    onFailure = { setFailure(it) },
                )
            }
        }
    }
}
