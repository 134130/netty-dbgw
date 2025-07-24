package com.github.l34130.netty.dbgw.protocol.postgres.command

import com.github.l34130.netty.dbgw.protocol.postgres.Message

class Query(
    val query: String,
) {
    override fun toString(): String = "Query(query='$query')"

    companion object {
        const val TYPE: Char = 'Q'

        fun readFrom(msg: Message): Query {
            require(msg.type == TYPE) {
                "Expected 'Q' for Query, but got ${msg.type}"
            }

            val query = msg.content.toString(Charsets.UTF_8)
            return Query(query)
        }
    }
}
