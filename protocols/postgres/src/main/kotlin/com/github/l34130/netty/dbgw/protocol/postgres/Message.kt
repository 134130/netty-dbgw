package com.github.l34130.netty.dbgw.protocol.postgres

import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.codec.MessageToByteEncoder

class MessageDecoder : ChannelInboundHandlerAdapter() {
    override fun channelRead(
        ctx: ChannelHandlerContext,
        msg: Any,
    ) {
        (msg as ByteBuf).retain()
        ctx.fireChannelRead(msg)
    }
}

class MessageEncoder : MessageToByteEncoder<ByteBuf>() {
    override fun encode(
        ctx: ChannelHandlerContext,
        msg: ByteBuf,
        out: ByteBuf,
    ) {
        if (msg.readerIndex() != 0) {
            msg.readerIndex(0) // Reset reader index to ensure we read from the start.
            logger.warn {
                "Message reader index is not at 0, resetting it. " +
                    "This may lead to unexpected behavior if the message is not fully read. " +
                    "Check your message handling logic."
            }
        }

        out.writeBytes(msg)
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}
