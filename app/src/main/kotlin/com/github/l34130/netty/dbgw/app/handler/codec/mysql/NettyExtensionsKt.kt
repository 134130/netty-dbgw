package com.github.l34130.netty.dbgw.app.handler.codec.mysql

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import io.netty.handler.codec.Delimiters

fun ByteBuf.readFixedLengthInteger(length: Int): Long {
    val bytes = ByteArray(length)
    readBytes(bytes)
    var value = 0L
    for (i in bytes.indices) {
        value = value or (bytes[i].toLong() and 0xFFL shl (i * 8))
    }
    return value
}

fun ByteBuf.writeFixedLengthInteger(
    length: Int,
    value: Long,
): ByteBuf {
    require(length in 1..8) { "Length must be between 1 and 8." }
    require(value >= 0) { "Value must be a non-negative integer." }
    require(value < (1L shl (length * 8))) { "Value exceeds the maximum for the specified length." }

    for (i in 0 until length) {
        writeByte((value shr (i * 8)).toInt() and 0xFF)
    }
    return this
}

fun ByteBuf.readFixedLengthString(length: Int): String {
    val bytes = ByteArray(length)
    readBytes(bytes)
    return String(bytes, Charsets.UTF_8)
}

fun ByteBuf.writeFixedLengthString(
    length: Int,
    value: String,
): ByteBuf {
    require(value.length <= length) { "Value exceeds the maximum length of $length characters." }
    val bytes = value.toByteArray(Charsets.UTF_8)
    writeBytes(bytes)
    // Fill the remaining space with null bytes if necessary
    if (bytes.size < length) {
        writeZero(length - bytes.size)
    }
    return this
}

// https://dev.mysql.com/doc/dev/mysql-server/latest/page_protocol_basic_dt_integers.html#sect_protocol_basic_dt_int_le
fun ByteBuf.readLenEncInteger(): Long {
    val firstByte = readUnsignedByte().toInt()
    return when {
        firstByte < 0xFB -> firstByte.toLong()
        firstByte == 0xFC -> readShortLE().toLong()
        firstByte == 0xFD -> readMediumLE().toLong()
        firstByte == 0xFE -> readLongLE()
        else -> throw IllegalArgumentException("Invalid length encoded integer prefix: $firstByte")
    }
}

private val NULL_BYTE_BUF = Delimiters.nulDelimiter()[0]

fun ByteBuf.readNullTerminatedString(): ByteBuf {
    val index = ByteBufUtil.indexOf(NULL_BYTE_BUF, this)
    if (index < 0) {
        throw IndexOutOfBoundsException("No null terminator found in ByteBuf.")
    }
    val read = readSlice(index - readerIndex())
    skipBytes(1) // Skip the null terminator byte
    return read
}

fun ByteBuf.readRestOfPacketString(): ByteBuf {
    val length = readableBytes()
    return when {
        length < 0 -> NULL_BYTE_BUF
        length == 0 -> NULL_BYTE_BUF
        else -> {
            readSlice(length)
        }
    }
}

fun ByteBuf.readLenEncString(): ByteBuf {
    val length = readLenEncInteger()
    return when {
        length < 0 -> NULL_BYTE_BUF
        length == 0L -> NULL_BYTE_BUF
        else -> {
            readBytes(length.toInt())
        }
    }
}
