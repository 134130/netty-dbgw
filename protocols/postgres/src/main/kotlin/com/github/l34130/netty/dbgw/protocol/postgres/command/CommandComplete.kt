package com.github.l34130.netty.dbgw.protocol.postgres.command

import com.github.l34130.netty.dbgw.protocol.postgres.Message

class CommandComplete(
    val commandTag: String,
) {
    override fun toString(): String = "CommandComplete()"

    companion object {
        const val TYPE: Char = 'C'

        fun readFrom(msg: Message): CommandComplete {
            require(msg.type == TYPE) { "Expected $TYPE, but got ${msg.type}" }
            val content = msg.content
            val commandTag = content.toString(Charsets.UTF_8)
            return CommandComplete(commandTag)
        }
    }
}
