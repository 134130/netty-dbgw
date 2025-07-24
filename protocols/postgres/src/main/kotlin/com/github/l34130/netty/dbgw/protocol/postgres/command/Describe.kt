package com.github.l34130.netty.dbgw.protocol.postgres.command

import com.github.l34130.netty.dbgw.protocol.postgres.Message
import com.github.l34130.netty.dbgw.protocol.postgres.readUntilNull

class Describe(
    val targetType: TargetType,
    val targetName: String,
) {
    override fun toString(): String = "Describe(targetType=$targetType, targetName='$targetName')"

    companion object {
        const val TYPE: Char = 'D'

        fun readFrom(msg: Message): Describe {
            require(msg.type == TYPE) { "Expected $TYPE, but got ${msg.type}" }

            val content = msg.content
            val targetType = TargetType.of(content.readByte())
            val targetName = content.readUntilNull().toString(Charsets.UTF_8)

            return Describe(targetType, targetName)
        }
    }

    enum class TargetType(
        val code: Char,
    ) {
        PREPARED_STATEMENT('S'),
        PORTAL('P'),
        ;

        companion object {
            fun of(code: Byte): TargetType = of(code.toInt().toChar())

            fun of(code: Char): TargetType =
                entries.find { it.code == code }
                    ?: throw IllegalArgumentException("Unknown target type: '$code'")
        }
    }
}
