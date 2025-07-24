package com.github.l34130.netty.dbgw.protocol.postgres.message

import io.netty.buffer.ByteBuf

class SimpleQuery(
    val query: String,
) {
    override fun toString(): String = "SimpleQuery(query='$query')"

    companion object {
        fun readFrom(buf: ByteBuf): SimpleQuery {
            require(buf.readByte().toInt() == 'Q'.code) {
                "Expected 'Q' for SimpleQuery, but got ${buf.readByte().toInt()}"
            }

            val length = buf.readInt()
            check(buf.readableBytes() - length - 5 >= 0) {
                "Expected at least ${length + 5} bytes for SimpleQuery, but got ${buf.readableBytes()}"
            }

            val query = buf.toString(Charsets.UTF_8)
            return SimpleQuery(query)
        }
    }
}
