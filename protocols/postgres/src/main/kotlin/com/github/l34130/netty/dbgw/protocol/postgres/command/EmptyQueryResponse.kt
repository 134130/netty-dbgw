package com.github.l34130.netty.dbgw.protocol.postgres.command

import com.github.l34130.netty.dbgw.protocol.postgres.Message

class EmptyQueryResponse {
    companion object {
        const val TYPE: Char = 'I'

        fun readFrom(msg: Message): EmptyQueryResponse {
            require(msg.type == TYPE) {
                "Expected $TYPE, but got ${msg.type}"
            }
            return EmptyQueryResponse()
        }
    }
}
