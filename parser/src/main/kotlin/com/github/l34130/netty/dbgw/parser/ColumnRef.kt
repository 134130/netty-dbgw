package com.github.l34130.netty.dbgw.parser

sealed interface ColumnRef {
    val columnName: String
}

data class DirectColumnRef(
    val tableSource: TableDefinition,
    override val columnName: String,
) : ColumnRef

data class DelayedColumnRef(
    val tableSourceCandidates: Set<TableDefinition>,
    override val columnName: String,
) : ColumnRef
