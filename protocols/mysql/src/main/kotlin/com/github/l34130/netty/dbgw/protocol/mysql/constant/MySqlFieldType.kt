package com.github.l34130.netty.dbgw.protocol.mysql.constant

import java.sql.Types

// https://dev.mysql.com/doc/dev/mysql-server/latest/field__types_8h.html
internal enum class MySqlFieldType(
    val value: Int,
) {
    MYSQL_TYPE_DECIMAL(0x00),
    MYSQL_TYPE_TINY(0x01),
    MYSQL_TYPE_SHORT(0x02),
    MYSQL_TYPE_LONG(0x03),
    MYSQL_TYPE_FLOAT(0x04),
    MYSQL_TYPE_DOUBLE(0x05),
    MYSQL_TYPE_NULL(0x06),
    MYSQL_TYPE_TIMESTAMP(0x07),
    MYSQL_TYPE_LONGLONG(0x08),
    MYSQL_TYPE_INT24(0x09),
    MYSQL_TYPE_DATE(0x0A),
    MYSQL_TYPE_TIME(0x0B),
    MYSQL_TYPE_DATETIME(0x0C),
    MYSQL_TYPE_YEAR(0x0D),

    // Internal to MySQL. Not used in protocol
    MYSQL_TYPE_NEWDATE(0x0E),
    MYSQL_TYPE_VARCHAR(0x0F),
    MYSQL_TYPE_BIT(0x10),
    MYSQL_TYPE_TIMESTAMP2(0x11),

    // Internal to MySQL. Not used in protocol
    MYSQL_TYPE_DATETIME2(0x12),

    // Internal to MySQL. Not used in protocol
    MYSQL_TYPE_TIME2(0x13),

    // Used for replication only.
    MYSQL_TYPE_TYPED_ARRAY(0x14),
    MYSQL_TYPE_VECTOR(242),
    MYSQL_TYPE_INVALID(243),

    // Currently just a placeholder.
    MYSQL_TYPE_BOOL(244),
    MYSQL_TYPE_JSON(245),
    MYSQL_TYPE_NEWDECIMAL(246),
    MYSQL_TYPE_ENUM(247),
    MYSQL_TYPE_SET(248),
    MYSQL_TYPE_TINY_BLOB(249),
    MYSQL_TYPE_MEDIUM_BLOB(250),
    MYSQL_TYPE_LONG_BLOB(251),
    MYSQL_TYPE_BLOB(252),
    MYSQL_TYPE_VAR_STRING(253),
    MYSQL_TYPE_STRING(254),
    MYSQL_TYPE_GEOMETRY(255),
    ;

    fun toJavaSqlType(): Int =
        when (this) {
            MYSQL_TYPE_DECIMAL -> Types.DECIMAL
            MYSQL_TYPE_TINY -> Types.TINYINT
            MYSQL_TYPE_SHORT -> Types.SMALLINT
            MYSQL_TYPE_LONG -> Types.INTEGER
            MYSQL_TYPE_FLOAT -> Types.FLOAT
            MYSQL_TYPE_DOUBLE -> Types.DOUBLE
            MYSQL_TYPE_NULL -> Types.NULL
            MYSQL_TYPE_TIMESTAMP, MYSQL_TYPE_TIMESTAMP2 -> Types.TIMESTAMP
            MYSQL_TYPE_LONGLONG -> Types.BIGINT
            MYSQL_TYPE_INT24 -> Types.INTEGER // MySQL's INT24 is equivalent to Java's INTEGER
            MYSQL_TYPE_DATE -> Types.DATE
            MYSQL_TYPE_TIME, MYSQL_TYPE_TIME2 -> Types.TIME
            MYSQL_TYPE_DATETIME, MYSQL_TYPE_DATETIME2 -> Types.TIMESTAMP
            MYSQL_TYPE_YEAR -> Types.SMALLINT // MySQL's YEAR is equivalent to Java's SMALLINT
            MYSQL_TYPE_VARCHAR, MYSQL_TYPE_VAR_STRING, MYSQL_TYPE_STRING -> Types.VARCHAR
            MYSQL_TYPE_BIT -> Types.BIT
            MYSQL_TYPE_JSON -> Types.OTHER
            MYSQL_TYPE_NEWDATE -> Types.DATE
            MYSQL_TYPE_TYPED_ARRAY -> Types.ARRAY
            MYSQL_TYPE_VECTOR -> Types.OTHER
            MYSQL_TYPE_INVALID -> Types.OTHER // Not a valid type, but we map it to OTHER
            MYSQL_TYPE_BOOL -> Types.BOOLEAN
            MYSQL_TYPE_NEWDECIMAL -> Types.DECIMAL
            MYSQL_TYPE_ENUM -> Types.VARCHAR
            MYSQL_TYPE_SET -> Types.VARCHAR
            MYSQL_TYPE_TINY_BLOB -> Types.BLOB
            MYSQL_TYPE_MEDIUM_BLOB -> Types.BLOB
            MYSQL_TYPE_LONG_BLOB -> Types.BLOB
            MYSQL_TYPE_BLOB -> Types.BLOB
            MYSQL_TYPE_GEOMETRY -> Types.OTHER
        }

    companion object {
        private val valuesMap = entries.associateBy(MySqlFieldType::value)

        fun of(value: Int): MySqlFieldType = valuesMap[value] ?: throw IllegalArgumentException("Unknown MySQL type value: $value")
    }
}
