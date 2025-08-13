package com.github.l34130.netty.dbgw.parser

import java.util.concurrent.ConcurrentHashMap

object TestUtils {
    private val tables = ConcurrentHashMap<String, PhysicalTableDefinition>()
    private val columns = ConcurrentHashMap<String, ConcurrentHashMap<String, ColumnRef>>()

    fun directColumnRef(
        table: String,
        column: String,
        tableAlias: String? = null,
    ): ColumnRef {
        val tableKey = "$table:$tableAlias"
        return this.columns
            .computeIfAbsent(tableKey) {
                this.tables.computeIfAbsent(tableKey) {
                    PhysicalTableDefinition(tableName = table, alias = tableAlias)
                }
                ConcurrentHashMap()
            }.computeIfAbsent(column) {
                DirectColumnRef(this.tables[tableKey]!!, column)
            }
    }

    fun delayedColumnRef(
        tables: List<String>,
        column: String,
        tableAlias: String? = null,
    ): ColumnRef {
        val tableKey = "${tables.joinToString(prefix = "[", postfix = "]")}:$tableAlias"
        return this.columns
            .computeIfAbsent(tableKey) {
                tables.forEach { table ->
                    this.tables.computeIfAbsent(tableKey) {
                        PhysicalTableDefinition(tableName = table, alias = tableAlias)
                    }
                }
                ConcurrentHashMap()
            }.computeIfAbsent(column) {
                DelayedColumnRef(
                    tableSourceCandidates = tables.map { this.tables["$it:$tableAlias"]!! }.toSet(),
                    columnName = column,
                )
            }
    }

    fun debugDump(
        sql: String,
        expected: ParseResult,
        actual: ParseResult,
    ): String =
        buildString {
            appendLine("=====================   SQL    =====================")
            appendLine(sql)

            appendLine("===================== Expected =====================")
            appendLine("Select Items:")
            expected.selectItems.forEach { selectItem ->
                printSelectItem(this, selectItem)
            }
            appendLine("Referenced Columns:")
            expected.referencedColumns.forEach { columnRef ->
                printSourceTree(this, columnRef)
            }

            appendLine("===================== Actual =======================")
            appendLine("Select Items:")
            actual.selectItems.forEach { selectItem ->
                printSelectItem(this, selectItem)
            }
            appendLine("Referenced Columns:")
            actual.referencedColumns.forEach { columnRef ->
                printSourceTree(this, columnRef)
            }
        }

    private fun printSelectItem(
        sb: StringBuilder,
        item: SelectItem,
        indent: String = "  ",
        visited: MutableSet<Any> = mutableSetOf(),
    ) {
        if (!visited.add(item)) return

        when (item) {
            is DirectColumn -> {
                sb.appendLine("$indent- [DirectColumn] ${item.columnRef.fqn()} (alias: ${item.alias ?: "N/A"})")
                printSourceTree(sb, item.columnRef, "", "$indent  ", visited)
            }
            is FunctionColumn -> {
                sb.appendLine(
                    "$indent- [FunctionColumn] ${item.functionName}(${item.arguments.joinToString()}) (alias: ${item.alias ?: "N/A"})",
                )
                item.sourceColumns.forEach { source ->
                    printSourceTree(sb, source, "[source] ", "$indent  ", visited)
                }
            }
        }
    }

    private fun printSourceTree(
        sb: StringBuilder,
        source: ColumnRef,
        label: String = "",
        indent: String = "  ",
        visited: MutableSet<Any> = mutableSetOf(),
    ) {
        if (!visited.add(source)) return

        sb.appendLine("$indent- $label${source.fqn()}")

        source
            .originColumns()
            .takeIf { it.isNotEmpty() && it.first() != source }
            ?.forEach { origin ->
                printSourceTree(sb, origin, "[col] ", "$indent  ", visited)
            }

        source
            .originReferences()
            .takeIf { it.isNotEmpty() && it.first() != source }
            ?.forEach { reference ->
                printSourceTree(sb, reference, "[ref] ", "$indent  ", visited)
            }
    }

    private fun TableDefinition.fqn(): String =
        buildString {
            if (this@fqn is PhysicalTableDefinition) {
                catalogName?.let { append("$it.") }
                schemaName?.let { append("$it.") }
            }
            append(name())
        }

    private fun ColumnRef.fqn(): String =
        buildString {
            when (this@fqn) {
                is DirectColumnRef -> {
                    append(tableSource.fqn())
                }
                is DelayedColumnRef -> {
                    append(tableSourceCandidates.joinToString(prefix = "[", postfix = "]") { it.fqn() })
                }
            }
            append(".")
            append(columnName)
        }

    private fun ColumnRef.originColumns(): Set<ColumnRef> =
        when (this) {
            is DirectColumnRef ->
                when (val tableSource = this.tableSource) {
                    is PhysicalTableDefinition -> setOf(this)
                    is DerivedTableDefinition -> tableSource.columns
                }
            is DelayedColumnRef -> emptySet() // TODO()
        }

    private fun ColumnRef.originReferences(): Set<ColumnRef> =
        when (this) {
            is DirectColumnRef ->
                when (val tableSource = this.tableSource) {
                    is PhysicalTableDefinition -> setOf(this)
                    is DerivedTableDefinition -> tableSource.references
                }
            is DelayedColumnRef -> emptySet() // TODO()
        }
}
