package com.github.l34130.netty.dbgw.protocol.postgres.command

import com.github.l34130.netty.dbgw.protocol.postgres.Message

class CloseComplete {
    override fun toString(): String = "CloseComplete()"

    companion object {
        const val TYPE: Char = '3'

        fun readFrom(msg: Message): CloseComplete {
            require(msg.type == TYPE) { "Expected $TYPE, but got ${msg.type}" }
            return CloseComplete()
        }
    }
}
