package com.github.l34130.netty.dbgw.core.utils

fun Int.toHexString(): String {
    val str = toString(16).uppercase()
    return if (str.length % 2 == 0) "0x$str" else "0x0$str"
}
