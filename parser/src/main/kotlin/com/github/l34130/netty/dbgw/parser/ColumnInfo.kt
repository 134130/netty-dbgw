package com.github.l34130.netty.dbgw.parser

sealed interface Source

data class PhysicalTableSource(
    val catalog: String? = null,
    val schema: String? = null,
    val table: String,
    val column: String,
) : Source

data class DerivedTableSource(
    val alias: String,
    val sourceColumn: String,
    val query: SelectParseResult,
) : Source

data class ColumnInfo(
    val name: String,
    val sources: List<Source>,
)

data class Table(
    val catalog: String? = null,
    val schema: String? = null,
    val name: String,
    val alias: String?,
)

sealed interface SqlParseResult

class SelectParseResult(
    val resultColumns: List<ColumnInfo>,
    val filterColumns: List<Source>,
)

class UpdateParseResult(
    val targetTable: Table,
    val updatedColumns: List<Source>,
    val filterColumns: List<Source>,
)

class DeleteParseResult(
    val targetTable: Table,
    val filterColumns: List<Source>,
)
