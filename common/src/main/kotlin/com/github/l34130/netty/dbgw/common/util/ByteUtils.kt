package com.github.l34130.netty.dbgw.common.util

object ByteUtils {
    fun xor(
        a: ByteArray,
        b: ByteArray,
    ): ByteArray {
        require(a.size == b.size) { "Byte arrays must be of the same length" }
        return ByteArray(a.size) { (a[it].toInt() xor b[it].toInt()).toByte() }
    }
}
