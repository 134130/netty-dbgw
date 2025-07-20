package com.github.l34130.netty.dbgw.app.handler.codec.mysql

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import io.netty.handler.codec.Delimiters

private val AVAILABLE_FIXED_LENGTH_INTEGER_LENGTHS = setOf(1, 2, 3, 4, 6, 8)

/**
 * Read a fixed-length unsigned integer stores its value in a series of bytes with the least significant byte first.
 *
 * https://dev.mysql.com/doc/dev/mysql-server/latest/page_protocol_basic_dt_integers.html#sect_protocol_basic_dt_int_fixed
 */
fun ByteBuf.readFixedLengthInteger(length: Int): ULong =
    when (length) {
        1 -> readUnsignedByte().toULong()
        2 -> readUnsignedShortLE().toULong()
        3 -> readUnsignedMediumLE().toULong()
        4 -> readUnsignedIntLE().toULong()
        6 -> {
            val low = readUnsignedShortLE().toULong()
            val high = readUnsignedIntLE().toULong()
            (high shl 16) or low
        }
        8 -> readLongLE().toULong()
        else -> throw IllegalArgumentException(
            "Unsupported length: $length, must be one of ${AVAILABLE_FIXED_LENGTH_INTEGER_LENGTHS.joinToString(", ")}.",
        )
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

// https://dev.mysql.com/doc/dev/mysql-server/latest/page_protocol_basic_dt_integers.html#sect_protocol_basic_dt_int_le

/**
 * Read an integer that consumes 1, 3, 4, or 9 bytes, depending on its numeric value
 */
fun ByteBuf.readLenEncInteger(): ULong {
    val firstByte = readUnsignedByte().toInt()
    return when {
        firstByte <= 0xFB -> firstByte.toULong()
        firstByte == 0xFC -> readShortLE().toULong()
        firstByte == 0xFD -> readMediumLE().toULong()
        firstByte == 0xFE -> readLongLE().toULong()
        else -> throw IllegalArgumentException("Invalid length encoded integer prefix: $firstByte")
    }
}

// https://dev.mysql.com/doc/dev/mysql-server/latest/page_protocol_basic_dt_strings.html#sect_protocol_basic_dt_string_fix
fun ByteBuf.readFixedLengthString(length: Int): String = readString(length, Charsets.UTF_8)

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

private val NULL_BYTE_BUF = Delimiters.nulDelimiter()[0]

fun ByteBuf.readNullTerminatedString(): ByteBuf {
    val index = ByteBufUtil.indexOf(NULL_BYTE_BUF, this)
    if (index < 0) {
        throw IndexOutOfBoundsException("No null terminator found in ByteBuf")
    }
    val read = readSlice(index - readerIndex())
    skipBytes(1) // Skip the null terminator byte
    return read
}

fun ByteBuf.readRestOfPacketString(): ByteBuf {
    val length = readableBytes()
    return when {
        length <= 0 -> Unpooled.EMPTY_BUFFER
        else -> readSlice(length)
    }
}

fun ByteBuf.readLenEncString(): ByteBuf {
    val length = readLenEncInteger()
    return when {
        length <= 0UL -> Unpooled.EMPTY_BUFFER
        else -> readSlice(length.toInt())
    }
}
