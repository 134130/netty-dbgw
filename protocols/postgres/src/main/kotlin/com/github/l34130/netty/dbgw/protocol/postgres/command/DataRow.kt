package com.github.l34130.netty.dbgw.protocol.postgres.command

import com.github.l34130.netty.dbgw.protocol.postgres.Message

class DataRow(
    // TODO: Optimize to use ByteBuf or similar for better performance
    val columnValues: List<String?>,
) {
    override fun toString(): String = "DataRow(columnValues=$columnValues)"

    companion object {
        const val TYPE: Char = 'D'

        fun readFrom(msg: Message): DataRow {
            require(msg.type == TYPE) { "Expected $TYPE, but got ${msg.type}" }

            val content = msg.content
            val columnCount = content.readShort().toInt()
            if (columnCount == 0) {
                return DataRow(emptyList())
            }

            val columnValues =
                List(columnCount) {
                    val length = content.readInt()
                    if (length == -1) {
                        null
                    } else {
                        content.readSlice(length).toString(Charsets.UTF_8)
                    }
                }

            return DataRow(columnValues)
        }
    }
}
