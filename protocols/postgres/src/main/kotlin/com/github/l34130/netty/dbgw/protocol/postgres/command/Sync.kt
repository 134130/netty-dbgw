package com.github.l34130.netty.dbgw.protocol.postgres.command

import com.github.l34130.netty.dbgw.protocol.postgres.Message

class Sync {
    override fun toString(): String = "Sync()"

    companion object {
        const val TYPE: Char = 'S' // Sync is represented by 'S' in the protocol

        fun readFrom(msg: Message): Sync {
            require(msg.type == TYPE) {
                "Expected 'S' for Sync, but got ${msg.type}"
            }
            return Sync()
        }
    }
}
