package com.github.l34130.netty.dbgw.protocol.mysql

import io.netty.buffer.ByteBuf

internal class Bitmap(
    val bits: ByteArray,
) {
    private val sizeInBits = bits.size * 8

    fun get(index: Int): Boolean {
        if (index < 0 || index >= sizeInBits) {
            throw IndexOutOfBoundsException("Index $index is out of bounds for bitmap of size $sizeInBits")
        }
        val byteIndex = index / 8
        val bitIndex = index % 8
        return (bits[byteIndex].toInt() and (1 shl bitIndex)) != 0
    }

    override fun toString(): String =
        buildString {
            append("Bitmap(bits=[")
            bits.forEachIndexed { index, byte ->
                append(byte.toString(2).padStart(8, '0'))
            }
            append("])")
        }

    companion object {
        fun readFrom(
            byteBuf: ByteBuf,
            length: Int,
        ): Bitmap {
            require(length >= 0) { "Length must be a non-negative integer." }
            val bits = ByteArray(length)
            byteBuf.readBytes(bits)
            return Bitmap(bits)
        }
    }
}
