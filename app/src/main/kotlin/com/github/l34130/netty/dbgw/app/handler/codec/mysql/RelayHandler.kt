package com.github.l34130.netty.dbgw.app.handler.codec.mysql

import com.github.l34130.netty.dbgw.utils.netty.closeOnFlush
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.buffer.ByteBufUtil
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.util.ReferenceCountUtil

class RelayHandler(
    private val proxyContext: ProxyContext,
    private val relayChannel: Channel,
    private val debugName: String? = null,
) : ChannelInboundHandlerAdapter() {
    private val logger = KotlinLogging.logger("RelayHandler-$debugName")

    override fun channelRead(
        ctx: ChannelHandlerContext,
        msg: Any?,
    ) {
        logger.warn {
            buildString {
                appendLine("Unhandled message: $msg")
                if (msg is Packet) {
                    msg.payload.markReaderIndex()
                    appendLine(ByteBufUtil.prettyHexDump(msg.payload))
                    msg.payload.resetReaderIndex()
                }
            }
        }
        if (relayChannel.isActive) {
            relayChannel.writeAndFlush(msg)
        } else {
            ReferenceCountUtil.release(msg)
            ctx.fireChannelReadComplete()
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
        logger.error(cause) { "Exception caught in $debugName" }
        ctx.channel().closeOnFlush()
    }
}
