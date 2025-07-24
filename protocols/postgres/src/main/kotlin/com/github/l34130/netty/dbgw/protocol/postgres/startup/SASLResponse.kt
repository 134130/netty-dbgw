package com.github.l34130.netty.dbgw.protocol.postgres.startup

import com.github.l34130.netty.dbgw.core.utils.ellipsize
import com.github.l34130.netty.dbgw.protocol.postgres.Message

class SASLResponse(
    val data: String,
) {
    override fun toString(): String = "SASLResponse(data='${data.ellipsize(20)}')"

    companion object {
        fun readFrom(msg: Message): SASLResponse {
            require(msg.type == 'p') {
                "Expected 'p', but got ${msg.type}"
            }

            val data = msg.content.toString(Charsets.UTF_8)
            return SASLResponse(data)
        }
    }
}
