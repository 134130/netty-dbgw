package com.github.l34130.netty.dbgw.protocol.postgres

import com.github.l34130.netty.dbgw.core.util.netty.writeUtf8
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import io.netty.handler.codec.Delimiters

private val NULL_BYTE_BUF = Delimiters.nulDelimiter()[0]

internal fun ByteBuf.readUntilNull(): ByteBuf {
    val index = ByteBufUtil.indexOf(NULL_BYTE_BUF, this)
    if (index < 0) {
        throw IndexOutOfBoundsException("No null terminator found in ByteBuf")
    }
    val read = readSlice(index - readerIndex())
    skipBytes(1) // Skip the null terminator byte
    return read
}

internal fun ByteBuf.writeNullTerminatedString(value: String) {
    writeUtf8(value)
    writeByte(0) // Write the null terminator
}
