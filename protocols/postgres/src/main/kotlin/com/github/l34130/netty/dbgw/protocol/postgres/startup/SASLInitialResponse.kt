package com.github.l34130.netty.dbgw.protocol.postgres.startup

import com.github.l34130.netty.dbgw.protocol.postgres.Message
import com.github.l34130.netty.dbgw.protocol.postgres.MessageConvertible
import com.github.l34130.netty.dbgw.protocol.postgres.SaslUtils
import com.github.l34130.netty.dbgw.protocol.postgres.readUntilNull
import com.github.l34130.netty.dbgw.protocol.postgres.writeNullTerminatedString
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled

data class SASLInitialResponse(
    val mechanism: String,
    val attributes: Map<String, String>,
) : MessageConvertible {
    override fun asMessage(): Message {
        val buf = Unpooled.buffer()

        buf.writeNullTerminatedString(mechanism)

        val data = attributes.let { SaslUtils.encodeSaslScramAttributes(it) }

        if (data.isEmpty()) {
            buf.writeInt(-1) // No initial client response
        } else {
            buf.writeInt(data.length)
            ByteBufUtil.writeAscii(buf, data)
        }

        return Message(
            type = TYPE, // 'p' for SASL Initial Response
            content = buf,
        )
    }

    override fun toString(): String = "SASLInitialResponse(mechanism='$mechanism', attributes=$attributes)"

    companion object {
        const val TYPE = 'p'

        fun readFrom(msg: Message): SASLInitialResponse {
            require(msg.type == TYPE) {
                "Expected '$TYPE', but got ${msg.type}"
            }

            val content = msg.content
            val mechanism = content.readUntilNull().toString(Charsets.UTF_8)
            val initialClientResponseLength = content.readInt()
            val initialResponse =
                if (initialClientResponseLength == -1) {
                    null // No initial client response
                } else {
                    content.readSlice(initialClientResponseLength).toString(Charsets.US_ASCII)
                }

            return SASLInitialResponse(mechanism, initialResponse?.let { SaslUtils.decodeSaslScramAttributes(it) } ?: emptyMap())
        }
    }
}
