package com.github.l34130.netty.dbgw.protocol.postgres

import io.netty.buffer.ByteBuf
import io.netty.util.ReferenceCounted

class Message(
    val type: Char,
    val content: ByteBuf,
) : ReferenceCounted {
    override fun toString(): String = "Message(type=$type, content=${content.readableBytes()} bytes)"

    override fun refCnt(): Int = content.refCnt()

    override fun retain(): ReferenceCounted? = apply { content.retain() }

    override fun retain(increment: Int): ReferenceCounted? = apply { content.retain(increment) }

    override fun touch(): ReferenceCounted? = apply { content.touch() }

    override fun touch(hint: Any?): ReferenceCounted? = apply { content.touch(hint) }

    override fun release(): Boolean = content.release()

    override fun release(decrement: Int): Boolean = content.release(decrement)
}
