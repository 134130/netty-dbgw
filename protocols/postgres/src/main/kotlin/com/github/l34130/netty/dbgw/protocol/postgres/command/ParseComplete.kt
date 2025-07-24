package com.github.l34130.netty.dbgw.protocol.postgres.command

import com.github.l34130.netty.dbgw.protocol.postgres.Message

class ParseComplete {
    override fun toString(): String = "ParseComplete()"

    companion object {
        const val TYPE: Char = '1' // ParseComplete is represented by '1' in the protocol

        fun readFrom(msg: Message): ParseComplete {
            require(msg.type == TYPE) {
                "Expected '1' for ParseComplete, but got ${msg.type}"
            }
            return ParseComplete()
        }
    }
}
