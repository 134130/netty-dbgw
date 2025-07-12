package com.github.l34130.netty.dbgw.app.handler.codec.mysql.data

class FixedLengthString(
    val length: Int,
    val value: String,
) : Comparable<FixedLengthString> {
    init {
        require(value.length <= length) { "Value exceeds the maximum length of $length characters." }
    }

    override fun compareTo(other: FixedLengthString): Int = value.compareTo(other.value)

    companion object {
        fun fromBytes(bytes: ByteArray): FixedLengthString {
            val value = String(bytes, Charsets.UTF_8)
            return FixedLengthString(bytes.size, value)
        }
    }
}
