package com.github.l34130.netty.dbgw.protocol.postgres.startup

import com.github.l34130.netty.dbgw.core.utils.ellipsize
import com.github.l34130.netty.dbgw.protocol.postgres.Message
import com.github.l34130.netty.dbgw.protocol.postgres.readUntilNull

class SASLInitialResponse(
    val mechanism: String,
    val data: String? = null,
) {
    override fun toString(): String = "SASLInitialResponse(mechanism='$mechanism', data='${data?.ellipsize(20)}')"

    companion object {
        fun readFrom(msg: Message): SASLInitialResponse {
            require(msg.type == 'p') {
                "Expected 'p', but got ${msg.type}"
            }

            val content = msg.content
            val mechanism = content.readUntilNull().toString(Charsets.UTF_8)
            val initialClientResponseLength = content.readInt()
            val initialResponse =
                if (initialClientResponseLength == -1) {
                    null // No initial client response
                } else {
                    content.readSlice(initialClientResponseLength).toString(Charsets.UTF_8)
                }

            return SASLInitialResponse(mechanism, initialResponse)
        }
    }
}
