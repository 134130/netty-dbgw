package com.github.l34130.netty.dbgw.protocol.postgres.startup

import com.github.l34130.netty.dbgw.protocol.postgres.Message
import com.github.l34130.netty.dbgw.protocol.postgres.MessageConvertible
import com.github.l34130.netty.dbgw.protocol.postgres.SaslUtils
import io.netty.buffer.Unpooled

data class SASLResponse(
    val attributes: Map<String, String>,
) : MessageConvertible {
    override fun asMessage(): Message =
        Message(
            type = 'p', // 'p' for SASL Response
            content = Unpooled.copiedBuffer(SaslUtils.encodeSaslScramAttributes(attributes), Charsets.US_ASCII),
        )

    override fun toString(): String = "SASLResponse(attributes='$attributes')"

    companion object {
        fun readFrom(msg: Message): SASLResponse {
            require(msg.type == 'p') {
                "Expected 'p', but got ${msg.type}"
            }

            val data = msg.content.toString(Charsets.US_ASCII)
            val attributes = SaslUtils.decodeSaslScramAttributes(data)
            return SASLResponse(attributes)
        }
    }
}
