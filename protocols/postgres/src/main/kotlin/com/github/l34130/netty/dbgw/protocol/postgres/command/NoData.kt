package com.github.l34130.netty.dbgw.protocol.postgres.command

import com.github.l34130.netty.dbgw.protocol.postgres.Message

class NoData {
    companion object {
        val TYPE = 'n'

        fun readFrom(msg: Message): NoData {
            require(msg.type == TYPE) {
                "Expected $TYPE, but got ${msg.type}"
            }
            return NoData()
        }
    }
}
