package com.github.l34130.netty.dbgw.protocol.postgres.message

import com.github.l34130.netty.dbgw.protocol.postgres.Message
import com.github.l34130.netty.dbgw.protocol.postgres.readUntilNull

class ParameterStatusMessage(
    val parameterName: String,
    val parameterValue: String,
) {
    override fun toString(): String = "ParameterStatusMessage(parameterName='$parameterName', parameterValue='$parameterValue')"

    companion object {
        fun readFrom(msg: Message): ParameterStatusMessage {
            require(msg.type == 'S') {
                "Expected 'S' for ParameterStatusMessage, but got ${msg.type}"
            }

            val parameterName = msg.content.readUntilNull().toString(Charsets.UTF_8)
            val parameterValue = msg.content.readUntilNull().toString(Charsets.UTF_8)

            return ParameterStatusMessage(parameterName, parameterValue)
        }
    }
}
