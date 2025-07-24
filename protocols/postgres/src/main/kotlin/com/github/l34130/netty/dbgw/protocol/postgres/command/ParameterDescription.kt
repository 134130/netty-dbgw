package com.github.l34130.netty.dbgw.protocol.postgres.command

import com.github.l34130.netty.dbgw.protocol.postgres.Message

class ParameterDescription(
    // The OID of the parameter data type
    val parameterTypes: List<Int>,
) {
    override fun toString(): String = "ParameterDescription(parameterTypes=${parameterTypes.joinToString()})"

    companion object {
        const val TYPE: Char = 't'

        fun readFrom(msg: Message): ParameterDescription {
            require(msg.type == TYPE) { "Expected $TYPE, but got ${msg.type}" }

            val content = msg.content
            val parameterCount = content.readInt()
            val parameterTypes = List(parameterCount) { content.readInt() }

            return ParameterDescription(parameterTypes)
        }
    }
}
