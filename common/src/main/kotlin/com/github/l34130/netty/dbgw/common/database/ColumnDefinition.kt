package com.github.l34130.netty.dbgw.common.database

data class ColumnDefinition(
    val catalog: String = "<unknown>",
    val schema: String = "<unknown>",
    val table: String? = null,
    val column: String? = null,
    val orgTables: List<String>,
    val orgColumns: List<String>,
    val columnType: SqlType = SqlType.OTHER,
) {
    override fun toString(): String =
        buildString {
            appendLine()
            if (catalog == "<unknown>") {
                append("?")
            } else {
                append("$catalog")
            }
            if (schema == "<unknown>") {
                append(".?")
            } else {
                append(".$schema")
            }
            if (table != null) {
                append(".$table")
            } else {
                append(".?")
            }
            if (column != null) {
                append(".$column")
            } else {
                append(".?")
            }
            appendLine()

            appendLine("  orgTables: $orgTables")
            appendLine("  orgColumns: $orgColumns")
        }
}
