package com.github.l34130.netty.dbgw.protocol.postgres.message

import io.netty.buffer.ByteBuf

class AuthenticationOk {
    companion object {
        fun readFrom(buf: ByteBuf): AuthenticationOk {
            require(buf.readByte().toInt() == 'R'.code) {
                "Expected 'R' for AuthenticationOk, but got ${buf.readByte().toInt()}"
            }

            val length = buf.readInt()
            check(buf.readableBytes() - length - 5 == 0) {
                "Expected ${length + 5} bytes for AuthenticationOk, but got ${buf.readableBytes()}"
            }

            return AuthenticationOk()
        }
    }
}
