package com.github.l34130.netty.dbgw.app.handler.codec.mysql.data

import io.netty.buffer.ByteBuf

/**
 * A fixed-length unsigned integer stores its value in a series of bytes with the least significant byte first.
 */
class FixedLengthInteger(
    val length: Int,
    val value: Long,
) {
    constructor(length: Int, value: Int) : this(length, value.toLong())

    init {
        require(length in ALLOWED_LENGTHS) { "Length must be one of ${ALLOWED_LENGTHS.joinToString(", ")}." }
        require(value >= 0) { "Value must be a non-negative integer." }
        require(value < (1L shl (length * 8))) { "Value exceeds the maximum for the specified length." }
    }

    fun writeTo(byteBuf: ByteBuf) {
        for (i in 0 until length) {
            byteBuf.writeByte((value shr (i * 8)).toInt() and 0xFF)
        }
    }

    companion object {
        private val ALLOWED_LENGTHS = setOf(1, 2, 3, 4, 6, 8)

        /**
         * Creates a [FixedLengthInteger] from a byte array.
         *
         * @param bytes The byte array to convert.
         * @return A [FixedLengthInteger] with the value represented by the byte array.
         */
        fun fromBytes(bytes: ByteArray): FixedLengthInteger {
            var value = 0L
            for (i in bytes.indices) {
                value = value or (bytes[i].toLong() and 0xFFL shl (i * 8))
            }
            return FixedLengthInteger(bytes.size, value)
        }
    }
}
