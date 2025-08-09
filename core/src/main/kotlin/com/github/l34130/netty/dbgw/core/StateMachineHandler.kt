
package com.github.l34130.netty.dbgw.core

import com.github.l34130.netty.dbgw.core.utils.netty.closeOnFlush
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.util.ReferenceCountUtil
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class StateMachineHandler(
    private val stateMachine: StateMachine,
    private val direction: MessageDirection,
) : ChannelInboundHandlerAdapter() {
    private val logger = KotlinLogging.logger("StateMachineHandler-${direction.name}")
    private val pendingMessages = AtomicInteger()
    private val channelReadCompleted = AtomicBoolean(false)

    override fun channelRead(
        ctx: ChannelHandlerContext,
        msg: Any,
    ) {
        pendingMessages.incrementAndGet()

        val relay = getRelayChannel(ctx)
        val processPromise =
            when (direction) {
                MessageDirection.FRONTEND -> stateMachine.processFrontendMessage(ctx, msg)
                MessageDirection.BACKEND -> stateMachine.processBackendMessage(ctx, msg)
            }

        processPromise.addListener { processFuture ->
            try {
                if (!processFuture.isSuccess) {
                    if (ctx.channel().isActive) {
                        logger.error(processFuture.cause()) { "Failed to process message in ${direction.name.lowercase()} direction" }
                        ReferenceCountUtil.release(msg)
                        ctx.close()
                    }
                    return@addListener
                }

                when (val action: MessageAction = processFuture.resultNow() as MessageAction) {
                    MessageAction.Forward -> {
                        relay.write(msg) // Forward the message as is.
                    }
                    is MessageAction.Transform -> {
                        ReferenceCountUtil.release(msg) // Release the original message as we are replacing it with a new one.
                        relay.write(action.newMsg) // Forward the transformed message.
                    }
                    is MessageAction.Intercept -> {
                        ReferenceCountUtil.release(msg) // Release the original message as we are intercepting it.
                        ctx.writeAndFlush(action.msg) // Write the intercepted response back to the channel.
                    }
                    MessageAction.Drop -> {
                        ReferenceCountUtil.release(msg) // Release the original message as we are dropping it.
                    }
                    is MessageAction.Terminate -> {
                        ReferenceCountUtil.release(msg) // Release the original message as we are terminating the processing.
                        logger.info { "Terminating processing: ${action.reason ?: "no reason provided"}" }
                        ctx.channel().closeOnFlush() // Close the channel on flush.
                    }
                    MessageAction.Handled -> {
                        // The message has been handled and released by the state machine, so do nothing.
                    }
                }
            } finally {
                val remaining = pendingMessages.decrementAndGet()
                if (remaining == 0 && channelReadCompleted.compareAndSet(true, false)) {
                    relay.flush() // Flush the relay channel if no more pending messages.
                }
            }
        }
    }

    override fun channelReadComplete(ctx: ChannelHandlerContext) {
        val relay = getRelayChannel(ctx)
        if (pendingMessages.get() > 0) {
            channelReadCompleted.set(true) // Set the flag to true if there are pending messages to flush later.
        } else {
            relay.eventLoop().execute {
                relay.flush() // Flush immediately if no pending messages.
            }
        }
    }

    override fun exceptionCaught(
        ctx: ChannelHandlerContext,
        cause: Throwable,
    ) {
        if (ctx.channel().isActive) {
            logger.error(cause) { "Exception caught in ${direction.name.lowercase()} channel" }
            ctx.close()
        } else {
            // Because we are using auto-read, the channel will read before the channel is closing
            // in the next event loop iteration. So we can safely ignore the exception here.
        }
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        val relay = getRelayChannel(ctx)
        if (relay.isActive) {
            logger.info { "${direction.name} channel inactive, closing ${direction.opposite()} channel" }
            relay.closeOnFlush() // Close the relay channel on flush.
        }
    }

    override fun channelWritabilityChanged(ctx: ChannelHandlerContext) {
        logger.info { "${direction.name} channel writability changed" }
        val canWrite = ctx.channel().isWritable
        // Update the relay channel's auto-read setting based on writability.
        getRelayChannel(ctx).config().isAutoRead = canWrite
        ctx.fireChannelWritabilityChanged() // Propagate the writability change event.
    }

    private fun getRelayChannel(ctx: ChannelHandlerContext): Channel =
        when (direction) {
            MessageDirection.FRONTEND -> ctx.backend()
            MessageDirection.BACKEND -> ctx.frontend()
        }
}
