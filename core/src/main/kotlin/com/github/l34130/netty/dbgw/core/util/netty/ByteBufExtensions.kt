package com.github.l34130.netty.dbgw.core.util.netty

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled

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

/**
 * Replaces a portion of the ByteBuf with new data.
 * @param block A lambda that receives a ByteBuf and returns a new ByteBuf
 * containing the data to replace the specified portion.
 * The read bytes read from the ByteBuf will be used to determine the length of the replacement.
 * If the block returns null or reads no bytes, the original ByteBuf is returned unchanged.
 * @return A new ByteBuf with the specified portion replaced, or the original ByteBuf
 */
fun ByteBuf.readAndReplace(block: (ByteBuf) -> ByteBuf?): ByteBuf {
    val duplicated = duplicate()
    val startIndex = duplicated.readerIndex()

    val newData = block(duplicated)

    val endIndex = duplicated.readerIndex()
    val readLength = endIndex - startIndex

    if (newData == null || newData.readableBytes() == 0 || readLength == 0) {
        // If no new data is provided or nothing was read, return the original buffer
        return this
    }

    return replace(startIndex, readLength, newData)
}

/**
 * Replaces a portion of the ByteBuf with the provided data.
 * @param index The starting index of the portion to replace.
 * @param length The length of the portion to replace.
 * @param data The ByteBuf containing the new data to insert.
 * @return A new ByteBuf with the specified portion replaced.
 * @throws IndexOutOfBoundsException if the index or length is out of bounds.
 */
fun ByteBuf.replace(
    index: Int,
    length: Int,
    data: ByteBuf,
): ByteBuf {
    val readableBytes = this.readableBytes()
    if (index < 0 || length < 0 || index + length > readableBytes) {
        throw IndexOutOfBoundsException("Index $index with length $length is out of bounds for ByteBuf with readable bytes $readableBytes")
    }

    val before = this.slice(0, index)
    val after = this.slice(index + length, readableBytes - (index + length))

    val buf = Unpooled.compositeBuffer(3)
    buf.addComponents(true, before, data, after)
    return buf
}

fun ByteBuf.toByteArray(): ByteArray = ByteBufUtil.getBytes(this, this.readerIndex(), this.readableBytes(), false)

fun ByteBuf.writeUtf8(str: String) = ByteBufUtil.writeUtf8(this, str)
