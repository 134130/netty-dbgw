package com.github.l34130.netty.dbgw.protocol.postgres.command

import com.github.l34130.netty.dbgw.protocol.postgres.Message
import com.github.l34130.netty.dbgw.protocol.postgres.readUntilNull

class Bind(
    val portal: String,
    val statement: String,
    val parameterFormatCodes: List<Short>,
    val parameters: List<ByteArray>,
    val resultFormatCodes: List<Short>,
) {
    override fun toString(): String =
        "Bind(portal='$portal', statement='$statement', parameterFormatCodes=$parameterFormatCodes, parameters=$parameters, resultFormatCodes=$resultFormatCodes)"

    companion object {
        const val TYPE: Char = 'B'

        fun readFrom(msg: Message): Bind {
            require(msg.type == TYPE) {
                "Expected 'B' for Bind, but got ${msg.type}"
            }

            val portal = msg.content.readUntilNull().toString(Charsets.UTF_8)
            val statement = msg.content.readUntilNull().toString(Charsets.UTF_8)

            val parameterFormatCodes = mutableListOf<Short>()
            while (msg.content.isReadable) {
                parameterFormatCodes.add(msg.content.readShort())
            }

            val parameters = mutableListOf<ByteArray>()
            while (msg.content.isReadable) {
                val length = msg.content.readInt()
                if (length < 0) {
                    parameters.add(ByteArray(0)) // Null parameter
                } else {
                    val parameter = ByteArray(length)
                    msg.content.readBytes(parameter)
                    parameters.add(parameter)
                }
            }

            val resultFormatCodes = mutableListOf<Short>()
            while (msg.content.isReadable) {
                resultFormatCodes.add(msg.content.readShort())
            }

            return Bind(portal, statement, parameterFormatCodes, parameters, resultFormatCodes)
        }
    }
}
