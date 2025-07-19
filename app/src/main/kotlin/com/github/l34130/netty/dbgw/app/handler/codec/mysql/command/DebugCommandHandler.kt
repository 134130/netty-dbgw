package com.github.l34130.netty.dbgw.app.handler.codec.mysql.command

import com.github.l34130.netty.dbgw.app.handler.codec.mysql.Packet
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.capabilities
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.downstream
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.readFixedLengthInteger
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.upstream
import com.github.l34130.netty.dbgw.utils.netty.peek
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler

class DebugCommandHandler : SimpleChannelInboundHandler<Packet>() {
    override fun channelRead0(
        ctx: ChannelHandlerContext,
        msg: Packet,
    ) {
        if (msg.payload.peek { it.readFixedLengthInteger(1) } == 0x0DUL) {
            logger.trace { "Received COM_DEBUG" }
            ctx.upstream().pipeline().addBefore(
                "relay-handler",
                "com-debug-response-handler",
                DebugCommandResponseHandler(),
            )
            ctx.upstream().writeAndFlush(msg)
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

    private class DebugCommandResponseHandler : SimpleChannelInboundHandler<Packet>() {
        override fun channelRead0(
            ctx: ChannelHandlerContext,
            msg: Packet,
        ) {
            logger.trace { "Received COM_DEBUG response" }
            when {
                msg.isOkPacket() -> {
                    logger.trace {
                        msg.payload.markReaderIndex()
                        "Received COM_DEBUG response: ${Packet.Ok.readFrom(msg.payload, ctx.capabilities().enumSet())}"
                        msg.payload.resetReaderIndex()
                    }
                }
                msg.isErrorPacket() -> {
                    logger.trace {
                        msg.payload.markReaderIndex()
                        "Received COM_DEBUG response: ${Packet.Error.readFrom(msg.payload, ctx.capabilities().enumSet())}"
                        msg.payload.resetReaderIndex()
                    }
                }
                else -> {
                    logger.warn { "Received unexpected COM_DEBUG response: $msg" }
                }
            }

            ctx.downstream().writeAndFlush(msg)
            ctx.pipeline().remove(this)
        }

        override fun handlerAdded(ctx: ChannelHandlerContext) {
            logger.trace { this::class.simpleName + " added to pipeline" }
        }

        override fun handlerRemoved(ctx: ChannelHandlerContext) {
            logger.trace { this::class.simpleName + " removed from pipeline" }
        }
    }
}
