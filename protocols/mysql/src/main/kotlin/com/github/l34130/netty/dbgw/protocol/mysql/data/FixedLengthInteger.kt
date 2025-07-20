package com.github.l34130.netty.dbgw.protocol.mysql.data

import io.netty.buffer.ByteBuf

/**
 * A fixed-length unsigned integer stores its value in a series of bytes with the least significant byte first.
 */
internal class FixedLengthInteger(
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
    }
}
