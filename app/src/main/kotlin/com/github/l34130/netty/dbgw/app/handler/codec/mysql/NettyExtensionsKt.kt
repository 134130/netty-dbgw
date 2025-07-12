package com.github.l34130.netty.dbgw.app.handler.codec.mysql

import com.github.l34130.netty.dbgw.app.handler.codec.mysql.data.FixedLengthInteger
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelFutureListener
import io.netty.handler.codec.Delimiters

fun ByteBuf.readFixedLengthInteger(length: Int): FixedLengthInteger {
    val bytes = ByteArray(length)
    readBytes(bytes)
    return FixedLengthInteger.fromBytes(bytes)
}

fun ByteBuf.readFixedLengthString(length: Int): String {
    val bytes = ByteArray(length)
    readBytes(bytes)
    return String(bytes, Charsets.UTF_8)
}

fun ByteBuf.readLengthEncodedInteger(): Long {
    val firstByte = readByte().toInt() and 0xFF
    return when {
        firstByte < 251 -> firstByte.toLong()
        firstByte == 251 -> -1L // NULL value
        firstByte == 252 -> readShortLE().toLong() and 0xFFFF
        firstByte == 253 -> readMediumLE().toLong() and 0xFFFFFF
        firstByte == 254 -> readLongLE()
        else -> throw IllegalArgumentException("Invalid length encoded integer prefix: $firstByte")
    }
}

private val NULL_BYTE_BUF = Delimiters.nulDelimiter()[0]

fun ByteBuf.readNullTerminatedString(): String {
    val index = ByteBufUtil.indexOf(NULL_BYTE_BUF, this)
    if (index < 0) {
        throw IndexOutOfBoundsException("No null terminator found in ByteBuf.")
    }

    return readString(index - readerIndex(), Charsets.UTF_8)
}

fun ByteBuf.readLengthEncodedString(): ByteBuf {
    val length = readLengthEncodedInteger()
    return when {
        length < 0 -> NULL_BYTE_BUF
        length == 0L -> NULL_BYTE_BUF
        else -> {
            readBytes(length.toInt())
        }
    }
}

fun Channel.closeOnFlush() {
    if (isActive) {
        writeAndFlush(Unpooled.EMPTY_BUFFER)
            .addListener(ChannelFutureListener.CLOSE)
    }
}
