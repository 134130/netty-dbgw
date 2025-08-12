package com.github.l34130.netty.dbgw.parser

sealed interface SelectItem {
    val alias: String?
    val sourceColumns: Set<ColumnRef>
}

data class DirectColumn(
    val columnRef: ColumnRef,
    private val customAlias: String? = null,
) : SelectItem {
    override val alias: String?
        get() = customAlias ?: columnRef.columnName
    override val sourceColumns: Set<ColumnRef>
        get() = setOf(columnRef)
}

data class FunctionColumn(
    val functionName: String,
    val arguments: List<String>,
    override val sourceColumns: Set<ColumnRef>,
    override val alias: String?,
) : SelectItem
