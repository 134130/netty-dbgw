package com.github.l34130.netty.dbgw.protocol.postgres.command

import com.github.l34130.netty.dbgw.protocol.postgres.Message
import com.github.l34130.netty.dbgw.protocol.postgres.readUntilNull

class Parse(
    // The name of the destination prepared statement (an empty string selects the unnamed prepared statement)
    val name: String,
    // The query string to be parsed
    val query: String,
    // The OIDs of the data types of the parameters in the query
    val parameterTypes: List<Int>,
) {
    override fun toString(): String = "Parse(name='$name', query='$query', parameterTypes=$parameterTypes)"

    companion object {
        const val TYPE: Char = 'P'

        fun readFrom(msg: Message): Parse {
            require(msg.type == TYPE) {
                "Expected '$TYPE' for Parse, but got ${msg.type}"
            }

            val content = msg.content

            // Read the name of the prepared statement
            val name = content.readUntilNull().toString(Charsets.UTF_8)
            // Read the query string
            val query = content.readUntilNull().toString(Charsets.UTF_8)

            // Read the OIDs of the parameter types
            val parameterCount = content.readShort()
            val parameterTypes = (0 until parameterCount).map { content.readInt() }

            return Parse(name, query, parameterTypes)
        }
    }
}
