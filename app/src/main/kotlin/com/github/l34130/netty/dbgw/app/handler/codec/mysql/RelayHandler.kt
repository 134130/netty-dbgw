package com.github.l34130.netty.dbgw.app.handler.codec.mysql

import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.util.ReferenceCountUtil

class RelayHandler(
    private val relayChannel: Channel,
    private val debugName: String? = null,
) : ChannelInboundHandlerAdapter() {
    override fun channelRead(
        ctx: ChannelHandlerContext,
        msg: Any?,
    ) {
        if (relayChannel.isActive) {
            when (msg) {
                is Packet -> {
                    println("$debugName| ${msg.sequenceId}: ${msg.payload.toString(Charsets.UTF_8)}")
                }
            }
            relayChannel.writeAndFlush(msg)
        } else {
            ReferenceCountUtil.release(msg)
        }
    }

    override fun channelWritabilityChanged(ctx: ChannelHandlerContext) {
        val canWrite = ctx.channel().isWritable
        relayChannel.config().isAutoRead = canWrite
        ctx.fireChannelWritabilityChanged()
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        if (relayChannel.isActive) {
            logger.info { " $debugName: Channel inactive, closing relay channel." }
            relayChannel.closeOnFlush()
        }
    }

    override fun exceptionCaught(
        ctx: ChannelHandlerContext,
        cause: Throwable?,
    ) {
        logger.error(cause) { "Exception caught in RelayHandler" }
        ctx.channel().closeOnFlush()
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}
