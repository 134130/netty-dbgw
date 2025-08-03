package com.github.l34130.netty.dbgw.common.sql

import java.math.BigDecimal
import java.sql.Types
import kotlin.reflect.KClass

@Suppress("SpellCheckingInspection")
enum class SqlType(
    val code: Int,
    val targetClass: KClass<*>,
) {
    BIT(Types.BIT, Boolean::class),
    TINYINT(Types.TINYINT, Byte::class),
    SMALLINT(Types.SMALLINT, Short::class),
    INTEGER(Types.INTEGER, Int::class),
    BIGINT(Types.BIGINT, Long::class),
    FLOAT(Types.FLOAT, Float::class),
    REAL(Types.REAL, Float::class),
    DOUBLE(Types.DOUBLE, Double::class),
    NUMERIC(Types.NUMERIC, BigDecimal::class),
    DECIMAL(Types.DECIMAL, BigDecimal::class),
    CHAR(Types.CHAR, String::class),
    VARCHAR(Types.VARCHAR, String::class),
    LONGVARCHAR(Types.LONGVARCHAR, String::class),
    DATE(Types.DATE, java.sql.Date::class),
    TIME(Types.TIME, java.sql.Time::class),
    TIMESTAMP(Types.TIMESTAMP, java.sql.Timestamp::class),
    BINARY(Types.BINARY, ByteArray::class),
    VARBINARY(Types.VARBINARY, ByteArray::class),
    LONGVARBINARY(Types.LONGVARBINARY, ByteArray::class),
    NULL(Types.NULL, Any::class),
    OTHER(Types.OTHER, Any::class),
    JAVA_OBJECT(Types.JAVA_OBJECT, Any::class),
    DISTINCT(Types.DISTINCT, Any::class),
    STRUCT(Types.STRUCT, Any::class),
    ARRAY(Types.ARRAY, Any::class),
    BLOB(Types.BLOB, ByteArray::class),
    CLOB(Types.CLOB, String::class),
    REF(Types.REF, Any::class),
    DATALINK(Types.DATALINK, String::class),
    BOOLEAN(Types.BOOLEAN, Boolean::class),
    ROWID(Types.ROWID, String::class),
    NCHAR(Types.NCHAR, String::class),
    NVARCHAR(Types.NVARCHAR, String::class),
    LONGNVARCHAR(Types.LONGNVARCHAR, String::class),
    NCLOB(Types.NCLOB, String::class),
    SQLXML(Types.SQLXML, String::class),
    REF_CURSOR(Types.REF_CURSOR, Any::class),
    TIME_WITH_TIMEZONE(Types.TIME_WITH_TIMEZONE, java.sql.Time::class),
    TIMESTAMP_WITH_TIMEZONE(Types.TIMESTAMP_WITH_TIMEZONE, java.sql.Timestamp::class),
    ;

    companion object {
        private val codeMap: Map<Int, SqlType> = entries.associateBy { it.code }

        fun fromCode(code: Int): SqlType? = codeMap[code]
    }
}
