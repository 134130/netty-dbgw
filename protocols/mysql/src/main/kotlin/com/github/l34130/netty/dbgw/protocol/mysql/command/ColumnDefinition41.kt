package com.github.l34130.netty.dbgw.protocol.mysql.command

import com.github.l34130.netty.dbgw.common.database.ColumnDefinition
import com.github.l34130.netty.dbgw.common.database.SqlType
import com.github.l34130.netty.dbgw.protocol.mysql.constant.MySqlFieldType
import com.github.l34130.netty.dbgw.protocol.mysql.readFixedLengthInteger
import com.github.l34130.netty.dbgw.protocol.mysql.readFixedLengthString
import com.github.l34130.netty.dbgw.protocol.mysql.readLenEncInteger
import com.github.l34130.netty.dbgw.protocol.mysql.readLenEncString
import io.netty.buffer.ByteBuf

// https://dev.mysql.com/doc/dev/mysql-server/latest/page_protocol_com_query_response_text_resultset_column_definition.html
internal class ColumnDefinition41(
    val catalog: String, // always "def"
    val schema: String, // database name
    val table: String, // virtual table name
    val orgTable: String, // physical table name
    val name: String, // virtual column name
    val orgName: String, // physical column name
    val lengthOfFixedLengthFields: Int, // [0x0C]
    val characterSet: Int, // character set number
    val columnLength: Int, // maximum length of the field
    val type: MySqlFieldType,
    val flags: Int,
    val decimals: Int, // max shown decimal digits
) {
    fun toColumnDefinition(): ColumnDefinition =
        ColumnDefinition(
            database = schema,
            schema = schema,
            table = orgTable,
            orgTable = orgTable,
            column = name,
            orgColumn = orgName,
            columnType = requireNotNull(SqlType.fromCode(type.toJavaSqlType())) { "Unsupported MySQL field type: $type" },
        )

    override fun toString(): String =
        buildString {
            append("ColumnDefinition41(catalog='$catalog', schema='$schema', table='$table', orgTable='$orgTable', ")
            append("name='$name', orgName='$orgName', characterSet=$characterSet, columnLength=$columnLength, type=$type, flags=$flags)")
        }

    companion object {
        fun readFrom(payload: ByteBuf): ColumnDefinition41 {
            val catalog = payload.readLenEncString().toString(Charsets.UTF_8) // always "def"
            val schema = payload.readLenEncString().toString(Charsets.UTF_8)
            val table = payload.readLenEncString().toString(Charsets.UTF_8) // virtual table name
            val orgTable = payload.readLenEncString().toString(Charsets.UTF_8) // physical table name
            val name = payload.readLenEncString().toString(Charsets.UTF_8) // virtual column name
            val orgName = payload.readLenEncString().toString(Charsets.UTF_8) // physical column name
            val lengthOfFixedLengthFields = payload.readLenEncInteger() // [0x0C]
            val characterSet = payload.readFixedLengthInteger(2).toInt()
            val columnLength = payload.readFixedLengthInteger(4).toInt() // maximum length of the field
            val type = MySqlFieldType.of(payload.readFixedLengthInteger(1).toInt())
            val flags = payload.readFixedLengthInteger(2).toInt()
            val decimals = payload.readFixedLengthInteger(1).toInt() // max shown decimal digits
            val reserved = payload.readFixedLengthString(2)

            return ColumnDefinition41(
                catalog = catalog,
                schema = schema,
                table = table,
                orgTable = orgTable,
                name = name,
                orgName = orgName,
                lengthOfFixedLengthFields = lengthOfFixedLengthFields.toInt(),
                characterSet = characterSet,
                columnLength = columnLength,
                type = type,
                flags = flags,
                decimals = decimals,
            )
        }
    }
}
