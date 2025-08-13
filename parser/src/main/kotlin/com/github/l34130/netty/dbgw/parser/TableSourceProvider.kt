package com.github.l34130.netty.dbgw.parser

fun interface TableSourceProvider {
    fun getTableSources(): Set<TableDefinition>
}

fun TableSourceProvider.resolveTable(tableName: String?): TableDefinition? {
    val tableSources = getTableSources()
    if (tableSources.size == 1) {
        val table = tableSources.first()

        return when (tableName) {
            null -> {
                // If there's only one table, we can return it directly without looking for the table name
                table
            }
            (table.alias() ?: table.name()) -> {
                // If the table name matches the table's name or alias, we can return it'
                table
            }
            else -> {
                // Otherwise, the table name doesn't match the table's name or alias
                null
            }
        }
    }

    return tableSources.find { (it.alias() ?: it.name()) == tableName }
}
