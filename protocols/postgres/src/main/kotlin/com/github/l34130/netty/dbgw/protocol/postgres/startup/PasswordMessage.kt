package com.github.l34130.netty.dbgw.protocol.postgres.startup

import com.github.l34130.netty.dbgw.core.utils.ellipsize
import com.github.l34130.netty.dbgw.protocol.postgres.Message

class PasswordMessage(
    val password: String,
) {
    override fun toString(): String = "PasswordMessage(password='${password.ellipsize(20)}')"

    companion object {
        const val TYPE: Char = 'p'

        fun readFrom(msg: Message): PasswordMessage {
            require(msg.type == TYPE) { "Expected $TYPE, but got ${msg.type}" }
            val password = msg.content.toString(Charsets.UTF_8)
            return PasswordMessage(password)
        }
    }
}
