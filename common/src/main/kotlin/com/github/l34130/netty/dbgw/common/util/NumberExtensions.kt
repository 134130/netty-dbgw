package com.github.l34130.netty.dbgw.common.util

fun Int.toHexString(): String {
    val str = toString(16).uppercase()
    return if (str.length % 2 == 0) "0x$str" else "0x0$str"
}

fun ByteArray.toHexString(): String = "0x" + joinToString(separator = "") { byte -> "%02x".format(byte) }.uppercase()
