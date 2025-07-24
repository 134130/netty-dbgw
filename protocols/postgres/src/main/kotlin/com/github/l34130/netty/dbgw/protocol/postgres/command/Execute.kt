package com.github.l34130.netty.dbgw.protocol.postgres.command

import com.github.l34130.netty.dbgw.protocol.postgres.Message
import com.github.l34130.netty.dbgw.protocol.postgres.readUntilNull

class Execute(
    val portal: String,
    val maxRows: Int = 0, // 0 means no limit
) {
    override fun toString(): String = "Execute(portal='$portal', maxRows=$maxRows)"

    companion object {
        const val TYPE: Char = 'E' // Execute is represented by 'E' in the protocol

        fun readFrom(msg: Message): Execute {
            require(msg.type == TYPE) {
                "Expected 'E' for Execute, but got ${msg.type}"
            }

            val portal = msg.content.readUntilNull().toString(Charsets.UTF_8)
            val maxRows = msg.content.readInt() // Read the max rows as an integer

            return Execute(portal, maxRows)
        }
    }
}
