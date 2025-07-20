package com.github.l34130.netty.dbgw.protocol.mysql.constant

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

    companion object {
        private val valuesMap = entries.associateBy(MySqlFieldType::value)

        fun of(value: Int): MySqlFieldType = valuesMap[value] ?: throw IllegalArgumentException("Unknown MySQL type value: $value")
    }
}
