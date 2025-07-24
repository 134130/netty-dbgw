package com.github.l34130.netty.dbgw.protocol.postgres.startup

import com.github.l34130.netty.dbgw.core.utils.ellipsize
import com.github.l34130.netty.dbgw.protocol.postgres.Message
import com.github.l34130.netty.dbgw.protocol.postgres.readUntilNull
import io.netty.buffer.ByteBufUtil

sealed class AuthenticationRequest {
    companion object {
        fun readFrom(msg: Message): AuthenticationRequest {
            require(msg.type == 'R') {
                "Expected 'R' for AuthenticationRequest, but got ${msg.type}"
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
                        val mechanism = content.readUntilNull().toString(Charsets.UTF_8)
                        if (mechanism.isNotEmpty()) {
                            mechanisms.add(mechanism)
                        }
                    }
                    AuthenticationSASL(mechanisms)
                }
                11 -> {
                    val data = content.toString(Charsets.UTF_8)
                    AuthenticationSASLContinue(data)
                }
                12 -> {
                    val data = content.toString(Charsets.UTF_8)
                    AuthenticationSASLFinal(data)
                }
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

    class AuthenticationSASLContinue(
        val data: String,
    ) : AuthenticationRequest() {
        override fun toString(): String = "AuthenticationSASLContinue(data='${data.ellipsize(20)}')"
    }

    class AuthenticationSASLFinal(
        val data: String,
    ) : AuthenticationRequest() {
        override fun toString(): String = "AuthenticationSASLFinal(data='${data.ellipsize(20)}')"
    }
}
