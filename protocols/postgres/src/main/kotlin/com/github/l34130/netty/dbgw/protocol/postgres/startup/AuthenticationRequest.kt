package com.github.l34130.netty.dbgw.protocol.postgres.startup

import com.github.l34130.netty.dbgw.protocol.postgres.Message
import com.github.l34130.netty.dbgw.protocol.postgres.MessageConvertible
import com.github.l34130.netty.dbgw.protocol.postgres.SaslUtils
import com.github.l34130.netty.dbgw.protocol.postgres.readUntilNull
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled

sealed class AuthenticationRequest {
    companion object {
        const val TYPE: Char = 'R'

        fun readFrom(msg: Message): AuthenticationRequest {
            require(msg.type == TYPE) {
                "Expected '$TYPE' for AuthenticationRequest, but got ${msg.type}"
            }

            val content = msg.content
            val type = content.readInt()
            return when (type) {
                0 -> AuthenticationOk
                5 -> {
                    val salt = ByteBufUtil.getBytes(content.readSlice(4))
                    AuthenticationMD5Password(salt)
                }
                10 -> {
                    val mechanisms = mutableListOf<String>()
                    while (content.isReadable) {
                        val mechanism = content.readUntilNull().toString(Charsets.US_ASCII)
                        if (mechanism.isNotEmpty()) {
                            mechanisms.add(mechanism)
                        }
                    }
                    AuthenticationSASL(mechanisms)
                }
                11 -> AuthenticationSASLContinue(SaslUtils.decodeSaslScramAttributes(content.toString(Charsets.US_ASCII)))
                12 -> AuthenticationSASLFinal(SaslUtils.decodeSaslScramAttributes(content.toString(Charsets.US_ASCII)))
                else -> TODO("Handle other authentication types: $type")
            }
        }
    }

    object AuthenticationOk : AuthenticationRequest() {
        override fun toString(): String = "AuthenticationOk"
    }

    class AuthenticationMD5Password(
        val salt: ByteArray,
    ) : AuthenticationRequest() {
        override fun toString(): String = "AuthenticationMD5Password(salt=${salt.joinToString { it.toString() }})"
    }

    class AuthenticationSASL(
        val mechanisms: List<String>,
    ) : AuthenticationRequest() {
        override fun toString(): String = "AuthenticationSASL(mechanisms=$mechanisms)"
    }

    data class AuthenticationSASLContinue(
        val attributes: Map<String, String>,
    ) : AuthenticationRequest(),
        MessageConvertible {
        override fun asMessage(): Message {
            val buf =
                Unpooled.buffer().apply {
                    writeInt(11)
                    ByteBufUtil.writeAscii(this, SaslUtils.encodeSaslScramAttributes(attributes))
                }
            return Message(
                type = TYPE, // 'R' for AuthenticationSASLContinue
                content = buf,
            )
        }

        override fun toString(): String = "AuthenticationSASLContinue(attributes=$attributes)"
    }

    class AuthenticationSASLFinal(
        val attributes: Map<String, String>,
    ) : AuthenticationRequest(),
        MessageConvertible {
        override fun asMessage(): Message {
            val buf =
                Unpooled.buffer().apply {
                    writeInt(12)
                    ByteBufUtil.writeAscii(this, SaslUtils.encodeSaslScramAttributes(attributes))
                }
            return Message(
                type = TYPE, // 'R' for AuthenticationSASLFinal
                content = buf,
            )
        }

        override fun toString(): String = "AuthenticationSASLFinal(attributes=$attributes)"
    }
}
