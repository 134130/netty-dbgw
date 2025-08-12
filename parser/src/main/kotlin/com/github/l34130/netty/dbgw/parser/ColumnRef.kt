package com.github.l34130.netty.dbgw.parser

data class ColumnRef(
    val tableSource: TableDefinition,
    val columnName: String,
)
