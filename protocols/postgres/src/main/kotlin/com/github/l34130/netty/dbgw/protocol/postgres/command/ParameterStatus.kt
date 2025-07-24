package com.github.l34130.netty.dbgw.protocol.postgres.command

import com.github.l34130.netty.dbgw.protocol.postgres.Message
import com.github.l34130.netty.dbgw.protocol.postgres.readUntilNull

class ParameterStatus(
    val parameterName: String,
    val parameterValue: String,
) {
    override fun toString(): String = "ParameterStatus(parameterName='$parameterName', parameterValue='$parameterValue')"

    companion object {
        const val TYPE: Char = 'S'

        fun readFrom(msg: Message): ParameterStatus {
            require(msg.type == TYPE) { "Expected $TYPE, but got ${msg.type}" }

            val content = msg.content
            val parameterName = content.readUntilNull().toString(Charsets.UTF_8)
            val parameterValue = content.readUntilNull().toString(Charsets.UTF_8)

            return ParameterStatus(parameterName, parameterValue)
        }
    }
}
