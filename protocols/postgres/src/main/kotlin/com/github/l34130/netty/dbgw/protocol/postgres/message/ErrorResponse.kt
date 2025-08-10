package com.github.l34130.netty.dbgw.protocol.postgres.message

import com.github.l34130.netty.dbgw.common.util.ellipsize
import com.github.l34130.netty.dbgw.protocol.postgres.Message
import com.github.l34130.netty.dbgw.protocol.postgres.constant.ErrorField
import com.github.l34130.netty.dbgw.protocol.postgres.readUntilNull
import com.github.l34130.netty.dbgw.protocol.postgres.writeNullTerminatedString
import io.netty.buffer.Unpooled

class ErrorResponse(
    val fields: List<Pair<ErrorField, String>>,
) {
    override fun toString(): String = "ErrorResponse(fields=${fields.joinToString { "${it.first}: ${it.second.ellipsize(50)}" }})"

    fun asMessage(): Message =
        Message(
            type = TYPE,
            content =
                Unpooled.buffer().apply {
                    for ((field, value) in fields) {
                        writeByte(field.code)
                        writeNullTerminatedString(value)
                    }
                    writeByte(0) // End of fields
                },
        )

    companion object {
        const val TYPE: Char = 'E' // ErrorResponse is represented by 'E' in the protocol

        fun readFrom(msg: Message): ErrorResponse {
            require(msg.type == TYPE) {
                "Expected 'E' for ErrorResponse, but got ${msg.type}"
            }

            val fields = mutableListOf<Pair<ErrorField, String>>()
            while (msg.content.isReadable) {
                val code = msg.content.readByte()
                if (code == 0.toByte()) {
                    break // End of fields
                }

                val type =
                    ErrorField.from(code)
                        ?: throw IllegalArgumentException("Unknown error field code: $code")

                val value = msg.content.readUntilNull().toString(Charsets.UTF_8)
                fields.add(type to value)
            }

            return ErrorResponse(fields)
        }

        fun of(
            severity: String,
            code: String,
            message: String,
        ): ErrorResponse =
            ErrorResponse(
                fields =
                    listOf(
                        ErrorField.S to severity,
                        ErrorField.C to code,
                        ErrorField.M to message,
                    ),
            )
    }
}
