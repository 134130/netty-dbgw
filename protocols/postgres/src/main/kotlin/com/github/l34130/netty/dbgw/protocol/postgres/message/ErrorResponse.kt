package com.github.l34130.netty.dbgw.protocol.postgres.message

import com.github.l34130.netty.dbgw.protocol.postgres.Message
import com.github.l34130.netty.dbgw.protocol.postgres.constant.ErrorField
import com.github.l34130.netty.dbgw.protocol.postgres.readUntilNull
import io.netty.buffer.Unpooled

class ErrorResponse(
    val type: ErrorField?,
    val value: String?,
) {
    override fun toString(): String = "ErrorResponse(type=$type, value='$value')"

    fun asMessage(): Message =
        Message(
            type = TYPE,
            content =
                Unpooled.buffer().apply {
                    writeByte(type?.code ?: 0) // Write the error field code, 0 if no error
                    if (value != null) {
                        writeBytes(value.toByteArray(Charsets.UTF_8)) // Write the error value
                    }
                    writeByte(0) // Null terminator for the string
                },
        )

    companion object {
        const val TYPE: Char = 'E' // ErrorResponse is represented by 'E' in the protocol

        fun readFrom(msg: Message): ErrorResponse {
            require(msg.type == TYPE) {
                "Expected 'E' for ErrorResponse, but got ${msg.type}"
            }

            val code = msg.content.readByte()
            if (code == 0.toByte()) {
                return ErrorResponse(null, null)
            }

            val type =
                ErrorField.from(code)
                    ?: throw IllegalArgumentException("Unknown error field code: $code")

            val value = msg.content.readUntilNull().toString(Charsets.UTF_8)
            return ErrorResponse(type, value)
        }
    }
}
