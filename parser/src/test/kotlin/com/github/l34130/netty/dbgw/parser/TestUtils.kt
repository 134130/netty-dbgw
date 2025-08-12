package com.github.l34130.netty.dbgw.parser

import java.util.concurrent.ConcurrentHashMap

object TestUtils {
    private val tables = ConcurrentHashMap<String, PhysicalTableDefinition>()
    private val columns = ConcurrentHashMap<String, ConcurrentHashMap<String, ColumnRef>>()

    fun column(
        table: String,
        column: String,
        tableAlias: String? = null,
        columnAlias: String? = null,
    ): ColumnRef {
        val tableKey = "$table:$tableAlias"
        return columns
            .computeIfAbsent(tableKey) {
                tables.computeIfAbsent(tableKey) {
                    PhysicalTableDefinition(tableName = table, alias = tableAlias)
                }
                ConcurrentHashMap()
            }.computeIfAbsent(column) {
                ColumnRef(tables[tableKey]!!, column)
            }
    }

    fun debugDump(
        sql: String,
        expected: ParseResult,
        actual: ParseResult,
    ): String =
        buildString {
            appendLine("SQL:\n  $sql")

            appendLine("===================== Expected =====================")
            appendLine("Columns:")
            expected.columns.forEach { columnRef ->
                printSourceTree(this, columnRef)
            }
            appendLine("Referenced Columns:")
            expected.referencedColumns.forEach { columnRef ->
                printSourceTree(this, columnRef)
            }

            appendLine("===================== Actual =======================")
            appendLine("Columns:")
            actual.columns.forEach { columnRef ->
                printSourceTree(this, columnRef)
            }
            appendLine("Referenced Columns:")
            actual.referencedColumns.forEach { columnRef ->
                printSourceTree(this, columnRef)
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
}
