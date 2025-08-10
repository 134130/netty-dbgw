package com.github.l34130.netty.dbgw.protocol.postgres.startup

import com.github.l34130.netty.dbgw.core.util.netty.toByteArray
import com.github.l34130.netty.dbgw.protocol.postgres.Message
import com.github.l34130.netty.dbgw.protocol.postgres.MessageConvertible
import com.github.l34130.netty.dbgw.protocol.postgres.readUntilNull
import io.netty.buffer.Unpooled

data class PasswordMessage(
    val password: ByteArray,
) : MessageConvertible {
    override fun asMessage(): Message =
        Message(
            type = TYPE,
            content = Unpooled.wrappedBuffer(password + byteArrayOf(0)),
        )

    override fun toString(): String = "PasswordMessage(password='$password')"

    companion object {
        const val TYPE: Char = 'p'

        fun readFrom(msg: Message): PasswordMessage {
            require(msg.type == TYPE) { "Expected $TYPE, but got ${msg.type}" }
            val password = msg.content.readUntilNull().toByteArray()
            return PasswordMessage(password)
        }
    }
}
