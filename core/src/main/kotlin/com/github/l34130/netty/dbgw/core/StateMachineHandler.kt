
package com.github.l34130.netty.dbgw.core

import com.github.l34130.netty.dbgw.core.utils.netty.closeOnFlush
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.util.ReferenceCountUtil

class StateMachineHandler(
    private val stateMachine: DatabaseStateMachine,
    private val direction: MessageDirection,
) : ChannelInboundHandlerAdapter() {
    private val logger = KotlinLogging.logger("StateMachineHandler-${direction.name}")

    override fun channelRead(
        ctx: ChannelHandlerContext,
        msg: Any,
    ) {
        val relay = getRelayChannel(ctx)
        val processPromise =
            when (direction) {
                MessageDirection.DOWNSTREAM -> stateMachine.processDownstreamMessage(ctx, msg)
                MessageDirection.UPSTREAM -> stateMachine.processUpstreamMessage(ctx, msg)
            }

        processPromise.addListener { processFuture ->
            if (!processFuture.isSuccess) {
                logger.error(processFuture.cause()) { "Failed to process message in ${direction.name.lowercase()} direction" }
                ctx.close()
                return@addListener
            }

            val action: MessageAction = processFuture.resultNow() as MessageAction

            val channelFuture =
                when (action) {
                    MessageAction.Forward -> relay.writeAndFlush(msg) // Forward the message as is.
                    is MessageAction.Transform -> {
                        ReferenceCountUtil.release(msg) // Release the original message as we are replacing it with a new one.
                        relay.writeAndFlush(action.newMsg) // Forward the transformed message.
                    }
                    is MessageAction.Intercept -> {
                        ReferenceCountUtil.release(msg) // Release the original message as we are intercepting it.
                        ctx.writeAndFlush(action.msg) // Write the intercepted response back to the channel.
                    }
                    MessageAction.Drop -> {
                        ReferenceCountUtil.release(msg) // Release the original message as we are dropping it.
                        ctx.newSucceededFuture()
                    }
                    is MessageAction.Terminate -> {
                        ReferenceCountUtil.release(msg) // Release the original message as we are terminating the processing.
                        logger.info { "Terminating processing: ${action.reason ?: "no reason provided"}" }
                        ctx.channel().closeOnFlush() // Close the channel on flush.
                    }
                }

            channelFuture.addListener { future ->
                if (!future.isSuccess) {
                    logger.error(future.cause()) { "Failed to process message in ${direction.name.lowercase()} direction" }
                    ctx.close()
                }
            }
        }
    }

    override fun exceptionCaught(
        ctx: ChannelHandlerContext,
        cause: Throwable,
    ) {
        if (ctx.channel().isActive) {
            logger.error(cause) { "Exception caught. Closing ${direction.name.lowercase()} channel" }
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
            MessageDirection.DOWNSTREAM -> ctx.upstream()
            MessageDirection.UPSTREAM -> ctx.downstream()
        }
}
