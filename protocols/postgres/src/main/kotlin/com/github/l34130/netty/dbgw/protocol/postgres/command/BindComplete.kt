package com.github.l34130.netty.dbgw.protocol.postgres.command

import com.github.l34130.netty.dbgw.protocol.postgres.Message

class BindComplete {
    override fun toString(): String = "BindComplete()"

    companion object {
        const val TYPE: Char = '2' // BindComplete is represented by '2' in the protocol

        fun readFrom(msg: Message): BindComplete {
            require(msg.type == TYPE) {
                "Expected '2' for BindComplete, but got ${msg.type}"
            }
            return BindComplete()
        }
    }
}
