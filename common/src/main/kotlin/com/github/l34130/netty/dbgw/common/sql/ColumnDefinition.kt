package com.github.l34130.netty.dbgw.common.sql

data class ColumnDefinition(
    val database: String,
    val schema: String,
    val table: String,
    val orgTable: String,
    val column: String,
    val orgColumn: String,
    val columnType: SqlType,
)
