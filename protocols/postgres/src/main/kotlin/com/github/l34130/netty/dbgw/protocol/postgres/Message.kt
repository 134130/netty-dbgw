package com.github.l34130.netty.dbgw.protocol.postgres

import io.netty.buffer.ByteBuf

class Message(
    val type: Char,
    val content: ByteBuf,
) {
    override fun toString(): String = "Message(type=$type, content=${content.readableBytes()} bytes)"
}
