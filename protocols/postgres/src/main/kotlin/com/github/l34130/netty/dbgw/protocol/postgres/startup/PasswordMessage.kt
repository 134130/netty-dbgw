package com.github.l34130.netty.dbgw.protocol.postgres.startup

import com.github.l34130.netty.dbgw.core.util.netty.toByteArray
import com.github.l34130.netty.dbgw.protocol.postgres.Message
import com.github.l34130.netty.dbgw.protocol.postgres.MessageConvertible
import com.github.l34130.netty.dbgw.protocol.postgres.readUntilNull
import io.netty.buffer.Unpooled
import java.security.MessageDigest

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

        private val MD5_PREFIX = "md5".toByteArray(Charsets.US_ASCII)

        fun readFrom(msg: Message): PasswordMessage {
            require(msg.type == TYPE) { "Expected $TYPE, but got ${msg.type}" }
            val password = msg.content.readUntilNull().toByteArray()
            return PasswordMessage(password)
        }

        fun ofMd5(
            username: String,
            password: String,
            salt: ByteArray,
        ): PasswordMessage {
            val password = MD5_PREFIX + md5Hex(md5Hex((password + username).toByteArray()) + salt)
            return PasswordMessage(password)
        }

        private fun md5Hex(input: ByteArray): ByteArray {
            val md5 = MessageDigest.getInstance("MD5")
            val digest = md5.digest(input)
            val hex = digest.joinToString("") { "%02x".format(it) }
            return hex.toByteArray()
        }
    }
}
