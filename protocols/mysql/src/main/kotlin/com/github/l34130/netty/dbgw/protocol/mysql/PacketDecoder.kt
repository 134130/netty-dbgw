package com.github.l34130.netty.dbgw.protocol.mysql

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder

internal class PacketDecoder : ByteToMessageDecoder() {
    override fun decode(
        ctx: ChannelHandlerContext,
        `in`: ByteBuf,
        out: MutableList<Any?>,
    ) {
        if (`in`.readableBytes() < HEADER_SIZE) {
            // Not enough bytes to read a complete packet header.
            return
        }

        `in`.markReaderIndex()

        val payloadLength = `in`.readFixedLengthInteger(3)
        val sequenceId = `in`.readFixedLengthInteger(1)

        if (`in`.readableBytes().toULong() < payloadLength) {
            // Not enough bytes to read the complete payload.
            `in`.resetReaderIndex()
            return
        }

        val payload = `in`.readRetainedSlice(payloadLength.toInt())
        out +=
            Packet(
                sequenceId = sequenceId.toInt(),
                payload = payload,
            )
    }

    companion object {
        const val HEADER_SIZE = 4
    }
}
