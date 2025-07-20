package com.github.l34130.netty.dbgw.utils.netty

import io.netty.buffer.ByteBuf

/**
 * Reads a slice of the ByteBuf without modifying the reader index.
 * This allows you to peek at the data without consuming it.
 * @param action The action to perform on the ByteBuf slice.
 * @return The result of the action, or null if the ByteBuf is empty.
 */
fun <T> ByteBuf.peek(action: (ByteBuf) -> T): T? {
    if (readableBytes() == 0) {
        return null
    }
    val currentReaderIndex = readerIndex()
    return try {
        try {
            action(readSlice(readableBytes()))
        } catch (e: IndexOutOfBoundsException) {
            // Handle IndexOutOfBoundsException gracefully
            null
        }
    } finally {
        readerIndex(currentReaderIndex) // Reset the reader index
    }
}
