package com.github.l34130.netty.dbgw.protocol.postgres.command

import com.github.l34130.netty.dbgw.common.database.ColumnDefinition
import com.github.l34130.netty.dbgw.common.database.SqlType
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
        fun toColumnDefinition(): ColumnDefinition =
            ColumnDefinition(
                catalog = "",
                schema = "",
                table = "",
                orgTables = emptyList(),
                column = name,
                orgColumns = emptyList(),
                columnType = Oid.fromCode(dataTypeId)?.type ?: SqlType.OTHER,
            )

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

        enum class Oid(
            val code: Int,
            val type: SqlType,
        ) {
            // Standard data types
            UNSPECIFIED(0, SqlType.OTHER),
            INT2(21, SqlType.SMALLINT),
            INT2_ARRAY(1005, SqlType.ARRAY),
            INT4(23, SqlType.INTEGER),
            INT4_ARRAY(1007, SqlType.ARRAY),
            INT8(20, SqlType.BIGINT),
            INT8_ARRAY(1016, SqlType.ARRAY),
            TEXT(25, SqlType.VARCHAR),
            TEXT_ARRAY(1009, SqlType.ARRAY),
            NUMERIC(1700, SqlType.NUMERIC),
            NUMERIC_ARRAY(1231, SqlType.ARRAY),
            FLOAT4(700, SqlType.REAL),
            FLOAT4_ARRAY(1021, SqlType.ARRAY),
            FLOAT8(701, SqlType.DOUBLE),
            FLOAT8_ARRAY(1022, SqlType.ARRAY),
            BOOL(16, SqlType.BOOLEAN),
            BOOL_ARRAY(1000, SqlType.ARRAY),
            DATE(1082, SqlType.DATE),
            DATE_ARRAY(1182, SqlType.ARRAY),
            TIME(1083, SqlType.TIME),
            TIME_ARRAY(1183, SqlType.ARRAY),
            TIMETZ(1266, SqlType.TIME_WITH_TIMEZONE),
            TIMETZ_ARRAY(1270, SqlType.ARRAY),
            TIMESTAMP(1114, SqlType.TIMESTAMP),
            TIMESTAMP_ARRAY(1115, SqlType.ARRAY),
            TIMESTAMPTZ(1184, SqlType.TIMESTAMP_WITH_TIMEZONE),
            TIMESTAMPTZ_ARRAY(1185, SqlType.ARRAY),
            BYTEA(17, SqlType.BINARY),
            BYTEA_ARRAY(1001, SqlType.ARRAY),
            VARCHAR(1043, SqlType.VARCHAR),
            VARCHAR_ARRAY(1015, SqlType.ARRAY),
            OID(26, SqlType.OTHER),
            OID_ARRAY(1028, SqlType.ARRAY),
            BPCHAR(1042, SqlType.CHAR),
            BPCHAR_ARRAY(1014, SqlType.ARRAY),
            MONEY(790, SqlType.OTHER),
            MONEY_ARRAY(791, SqlType.ARRAY),
            NAME(19, SqlType.VARCHAR),
            NAME_ARRAY(1003, SqlType.ARRAY),
            BIT(1560, SqlType.OTHER),
            BIT_ARRAY(1561, SqlType.ARRAY),
            VOID(2278, SqlType.OTHER),
            INTERVAL(1186, SqlType.OTHER),
            INTERVAL_ARRAY(1187, SqlType.ARRAY),
            CHAR(18, SqlType.CHAR),
            CHAR_ARRAY(1002, SqlType.ARRAY),
            VARBIT(1562, SqlType.OTHER),
            VARBIT_ARRAY(1563, SqlType.ARRAY),
            UUID(2950, SqlType.VARCHAR),
            UUID_ARRAY(2951, SqlType.ARRAY),
            XML(142, SqlType.SQLXML),
            XML_ARRAY(143, SqlType.ARRAY),
            POINT(600, SqlType.OTHER),
            POINT_ARRAY(1017, SqlType.ARRAY),
            BOX(603, SqlType.OTHER),
            BOX_ARRAY(1020, SqlType.ARRAY),
            JSONB(3802, SqlType.OTHER),
            JSONB_ARRAY(3807, SqlType.ARRAY),
            JSON(114, SqlType.OTHER),
            JSON_ARRAY(199, SqlType.ARRAY),
            REF_CURSOR(1790, SqlType.OTHER),
            REF_CURSOR_ARRAY(2201, SqlType.ARRAY),
            LINE(628, SqlType.OTHER),
            LSEG(601, SqlType.OTHER),
            PATH(602, SqlType.OTHER),
            POLYGON(604, SqlType.OTHER),
            CIRCLE(718, SqlType.OTHER),
            CIDR(650, SqlType.OTHER),
            INET(869, SqlType.OTHER),
            MACADDR(829, SqlType.OTHER),
            MACADDR8(774, SqlType.OTHER),
            TSVECTOR(3614, SqlType.OTHER),
            TSQUERY(3615, SqlType.OTHER),
            ;

            companion object {
                private val codeMap: Map<Int, Oid> = entries.associateBy { it.code }

                fun fromCode(code: Int): Oid? = codeMap[code]
            }
        }
    }
}
