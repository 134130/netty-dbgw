package com.github.l34130.netty.dbgw.app.handler.codec.mysql

import com.github.l34130.netty.dbgw.app.handler.codec.mysql.data.FixedLengthInteger
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder
import io.netty.handler.codec.MessageToByteEncoder

class PacketDecoder : ByteToMessageDecoder() {
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

        if (`in`.readableBytes() < payloadLength.value) {
            // Not enough bytes to read the complete payload.
            `in`.resetReaderIndex()
            return
        }

        val payload = `in`.readSlice(payloadLength.value.toInt())
        out +=
            Packet(
                payloadLength = payloadLength.value.toInt(),
                sequenceId = sequenceId.value.toInt(),
                payload = payload.retain(),
            )
    }

    companion object {
        const val HEADER_SIZE = 4
    }
}

class PacketEncoder : MessageToByteEncoder<Packet>() {
    override fun encode(
        ctx: ChannelHandlerContext,
        msg: Packet,
        out: ByteBuf,
    ) {
        FixedLengthInteger(3, msg.payload.readableBytes()).writeTo(out)
        FixedLengthInteger(1, msg.sequenceId).writeTo(out)
        out.writeBytes(msg.payload.nioBuffer())
    }
}

class Packet(
    val payloadLength: Int,
    val sequenceId: Int,
    val payload: ByteBuf,
) {
    override fun toString(): String =
        "Packet(payloadLength=$payloadLength, sequenceId=$sequenceId, payload=${payload.readableBytes()} bytes)"
}
