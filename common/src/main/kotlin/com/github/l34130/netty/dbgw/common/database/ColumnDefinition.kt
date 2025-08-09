package com.github.l34130.netty.dbgw.common.database

data class ColumnDefinition(
    val catalog: String,
    val schema: String,
    val table: String,
    val orgTable: String,
    val column: String,
    val orgColumn: String,
    val columnType: SqlType,
)
