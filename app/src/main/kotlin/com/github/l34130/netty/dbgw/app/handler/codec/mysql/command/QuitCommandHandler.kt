package com.github.l34130.netty.dbgw.app.handler.codec.mysql.command

import com.github.l34130.netty.dbgw.app.handler.codec.mysql.Packet
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.ProxyContext
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.readFixedLengthInteger
import com.github.l34130.netty.dbgw.utils.netty.closeOnFlush
import com.github.l34130.netty.dbgw.utils.netty.peek
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler

class QuitCommandHandler(
    private val proxyContext: ProxyContext,
) : SimpleChannelInboundHandler<Packet>() {
    override fun channelRead0(
        ctx: ChannelHandlerContext,
        msg: Packet,
    ) {
        if (msg.payload.peek { it.readFixedLengthInteger(1) } == 0x01UL) {
            logger.trace { "Received COM_QUIT" }
            ctx.pipeline().addBefore(
                "relay-handler",
                "com-quit-response-handler",
                QuitCommandResponseHandler(proxyContext),
            )
            proxyContext.upstream().writeAndFlush(msg)
        } else {
            ctx.fireChannelRead(msg)
        }
    }

    override fun handlerAdded(ctx: ChannelHandlerContext) {
        logger.trace { this::class.simpleName + " added to pipeline" }
    }

    override fun handlerRemoved(ctx: ChannelHandlerContext) {
        logger.trace { this::class.simpleName + " removed from pipeline" }
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }

    private class QuitCommandResponseHandler(
        private val proxyContext: ProxyContext,
    ) : SimpleChannelInboundHandler<Packet>() {
        override fun channelRead0(
            ctx: ChannelHandlerContext,
            msg: Packet,
        ) {
            logger.trace { "Received COM_QUIT response" }

            when {
                msg.isErrorPacket() -> {
                    logger.trace {
                        msg.payload.markReaderIndex()
                        "Received COM_QUIT response: ${Packet.Error.readFrom(msg.payload, proxyContext.capabilities())}"
                        msg.payload.resetReaderIndex()
                    }
                }
                else -> logger.warn { "Received unexpected COM_QUIT response: $msg" }
            }

            proxyContext.downstream().closeOnFlush()
        }

        override fun handlerAdded(ctx: ChannelHandlerContext) {
            logger.trace { this::class.simpleName + " added to pipeline" }
        }

        override fun handlerRemoved(ctx: ChannelHandlerContext) {
            logger.trace { this::class.simpleName + " removed from pipeline" }
        }
    }
}
