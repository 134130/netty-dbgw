package com.github.l34130.netty.dbgw.protocol.postgres

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder

class MessageDecoder : ByteToMessageDecoder() {
    override fun decode(
        ctx: ChannelHandlerContext,
        `in`: ByteBuf,
        out: MutableList<Any>,
    ) {
        if (`in`.readableBytes() < 5) {
            // Not enough bytes to read the message type and length.
            return
        }

        val messageType = `in`.readByte().toInt().toChar()

        val length = `in`.readInt() - 4 // Subtract 4 for the length field itself.
        if (`in`.readableBytes() < length) {
            // Not enough bytes to read the full message.
            `in`.resetReaderIndex() // Reset to read again later.
            return
        }

        val messageContent = `in`.readBytes(length)
        out += Message(messageType, messageContent)
    }
}
