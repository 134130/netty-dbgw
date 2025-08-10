@file:Suppress("ktlint:standard:filename")

package com.github.l34130.netty.dbgw.common.util

fun String.ellipsize(maxLength: Int): String {
    if (length <= maxLength) return this
    return "${take(maxLength - 1)}…"
}
