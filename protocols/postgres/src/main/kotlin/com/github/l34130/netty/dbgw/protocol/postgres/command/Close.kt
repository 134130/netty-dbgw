package com.github.l34130.netty.dbgw.protocol.postgres.command

import com.github.l34130.netty.dbgw.protocol.postgres.Message
import com.github.l34130.netty.dbgw.protocol.postgres.readUntilNull

class Close(
    val targetType: TargetType,
    val targetName: String,
) {
    override fun toString(): String = "Close(targetType='$targetType', targetName='$targetName')"

    companion object {
        const val TYPE: Char = 'C' // Close is represented by 'C' in the protocol

        fun readFrom(msg: Message): Close {
            require(msg.type == TYPE) { "Expected $TYPE, but got ${msg.type}" }

            val content = msg.content
            val targetType = TargetType.from(content.readByte())

            val targetName = content.readUntilNull().toString(Charsets.UTF_8)

            return Close(targetType, targetName)
        }
    }

    enum class TargetType(
        val type: Char,
    ) {
        PREPARED_STATEMENT('S'),
        PORTAL('P'),
        ;

        companion object {
            fun from(byte: Byte): TargetType = from(byte.toInt().toChar())

            fun from(type: Char): TargetType =
                entries.find { it.type == type }
                    ?: throw IllegalArgumentException("Unknown target type: '$type'")
        }
    }
}
