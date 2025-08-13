package com.github.l34130.netty.dbgw.parser

data class LineageContext(
    val parent: LineageContext? = null,
    val tableSources: MutableSet<TableDefinition> = mutableSetOf(),
    val selectItems: MutableSet<SelectItem> = mutableSetOf(),
    val referencedColumns: MutableSet<ColumnRef> = mutableSetOf(),
) {
    fun childContext(): LineageContext = LineageContext(this)

    fun resolveTable(tableName: String?): TableDefinition? {
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
}
