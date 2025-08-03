package com.github.l34130.netty.dbgw.protocol.postgres.command

import com.github.l34130.netty.dbgw.protocol.postgres.Message
import com.github.l34130.netty.dbgw.protocol.postgres.readUntilNull

class Bind(
    val portal: String,
    val statement: String,
    val parameterFormatCodes: List<Short>,
    val parameterValues: List<String>,
    val resultColumnFormatCodes: List<Short>,
) {
    override fun toString(): String =
        "Bind(portal='$portal', statement='$statement', parameterFormatCodes=$parameterFormatCodes, parameters=$parameterValues, resultFormatCodes=$resultColumnFormatCodes)"

    companion object {
        const val TYPE: Char = 'B'

        fun readFrom(msg: Message): Bind {
            require(msg.type == TYPE) {
                "Expected 'B' for Bind, but got ${msg.type}"
            }

            val portal = msg.content.readUntilNull().toString(Charsets.UTF_8)
            val statement = msg.content.readUntilNull().toString(Charsets.UTF_8)

            val parameterFormatCodesSize = msg.content.readShort()
            val parameterFormatCodes = mutableListOf<Short>()
            for (i in 0 until parameterFormatCodesSize) {
                parameterFormatCodes.add(msg.content.readShort())
            }

            val parameterValuesSize = msg.content.readShort()
            val parameterValues = mutableListOf<String>()
            for (i in 0 until parameterValuesSize) {
                val parameterValueLength = msg.content.readInt()
                if (parameterValueLength == -1) {
                    null
                } else {
                    val parameterValue = msg.content.readSlice(parameterValueLength).toString(Charsets.UTF_8)
                    parameterValues.add(parameterValue)
                }
            }

            val resultColumnFormatCodesSize = msg.content.readShort()
            val resultFormatCodes = mutableListOf<Short>()
            for (i in 0 until resultColumnFormatCodesSize) {
                resultFormatCodes.add(msg.content.readShort())
            }

            return Bind(portal, statement, parameterFormatCodes, parameterValues, resultFormatCodes)
        }
    }
}
