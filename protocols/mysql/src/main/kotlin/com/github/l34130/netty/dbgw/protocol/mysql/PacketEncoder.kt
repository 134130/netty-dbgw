package com.github.l34130.netty.dbgw.protocol.mysql

import com.github.l34130.netty.dbgw.protocol.mysql.data.FixedLengthInteger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder

class PacketEncoder : MessageToByteEncoder<Packet>() {
    override fun encode(
        ctx: ChannelHandlerContext,
        msg: Packet,
        out: ByteBuf,
    ) {
        if (msg.payload.readerIndex() != 0) {
            logger.warn {
                "Packet payload reader index is not at 0, resetting it. " +
                    "This may lead to unexpected behavior if the payload is not fully read. " +
                    "Check your packet handling logic."
            }
            msg.payload.readerIndex(0)
        }

        FixedLengthInteger(3, msg.payload.readableBytes()).writeTo(out)
        FixedLengthInteger(1, msg.sequenceId).writeTo(out)
        out.writeBytes(msg.payload)
        msg.payload.release()
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}
