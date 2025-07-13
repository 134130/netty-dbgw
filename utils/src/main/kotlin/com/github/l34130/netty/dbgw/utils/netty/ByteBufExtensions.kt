package com.github.l34130.netty.dbgw.utils.netty

import io.netty.buffer.ByteBuf
import java.io.EOFException

fun <T> ByteBuf.peek(action: (ByteBuf) -> T): T? {
    if (readableBytes() == 0) {
        return null
    }
    val currentReaderIndex = readerIndex()
    return try {
        try {
            action(readSlice(readableBytes()))
        } catch (e: EOFException) {
            // Handle EOFException gracefully, return null if no data is available
            null
        }
    } finally {
        readerIndex(currentReaderIndex) // Reset the reader index
    }
}
