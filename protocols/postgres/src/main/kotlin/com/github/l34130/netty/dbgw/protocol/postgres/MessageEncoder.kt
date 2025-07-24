package com.github.l34130.netty.dbgw.protocol.postgres

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder

class MessageEncoder : MessageToByteEncoder<Message>() {
    override fun encode(
        ctx: ChannelHandlerContext,
        msg: Message,
        out: ByteBuf,
    ) {
        msg.content.readerIndex(0) // Reset reader index to ensure we read from the start.

        out.writeByte(msg.type.code) // Write the message type byte.
        out.writeInt(msg.content.readableBytes() + 4) // Length includes the 4-byte length field itself.
        out.writeBytes(msg.content)

        msg.content.release() // Release the ByteBuf after writing to avoid memory leaks.
    }
}
