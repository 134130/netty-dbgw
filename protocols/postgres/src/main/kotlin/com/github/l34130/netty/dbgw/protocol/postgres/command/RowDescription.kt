package com.github.l34130.netty.dbgw.protocol.postgres.command

import com.github.l34130.netty.dbgw.protocol.postgres.Message
import com.github.l34130.netty.dbgw.protocol.postgres.readUntilNull
import io.netty.buffer.ByteBuf

class RowDescription(
    val fields: List<Field>,
) {
    override fun toString(): String = "RowDescription(fields=${fields.joinToString()})"

    companion object {
        const val TYPE: Char = 'T'

        fun readFrom(msg: Message): RowDescription {
            require(msg.type == TYPE) {
                "Expected $TYPE, but got ${msg.type}"
            }

            val content = msg.content
            val fieldCount = content.readShort().toInt()
            val fields: List<Field> =
                if (fieldCount > 0) {
                    List(fieldCount) { Field.readFrom(content) }
                } else {
                    emptyList()
                }
            return RowDescription(fields)
        }
    }

    class Field(
        // The name of the field
        val name: String,
        // The OID of the table
        val tableId: Int,
        // The column number (1-based) of the field in the table
        val columnId: Int,
        // The OID of the data type of the field
        val dataTypeId: Int,
        // The size of the data type in bytes, or negative if variable-length
        val dataTypeSize: Int,
        // The type modifier, which is implementation-dependent
        val typeModifier: Int,
        // The format code for the field, always 0
        val formatCode: Short,
    ) {
        override fun toString(): String =
            "Field(name='$name', tableId=$tableId, columnId=$columnId, dataTypeId=$dataTypeId, dataTypeSize=$dataTypeSize, typeModifier=$typeModifier)"

        companion object {
            fun readFrom(buf: ByteBuf): Field {
                val name = buf.readUntilNull().toString(Charsets.UTF_8)
                val tableId = buf.readInt()
                val columnId = buf.readShort().toInt()
                val dataTypeId = buf.readInt()
                val dataTypeSize = buf.readShort().toInt()
                val typeModifier = buf.readInt()
                val formatCode = buf.readShort()

                return Field(
                    name = name,
                    tableId = tableId,
                    columnId = columnId,
                    dataTypeId = dataTypeId,
                    dataTypeSize = dataTypeSize,
                    typeModifier = typeModifier,
                    formatCode = formatCode,
                )
            }
        }
    }
}
