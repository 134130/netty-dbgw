
package com.github.l34130.netty.dbgw.parser

import net.sf.jsqlparser.schema.Table
import net.sf.jsqlparser.statement.select.FromItemVisitorAdapter
import net.sf.jsqlparser.statement.select.SubSelect

class LineageFromItemVisitor : FromItemVisitorAdapter() {
    val tableDefinitions: MutableSet<TableDefinition> = mutableSetOf()

    fun resolveTable(tableName: String?): TableDefinition? {
        if (tableDefinitions.size == 1) {
            val table = tableDefinitions.first()

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

        return tableDefinitions.find { (it.alias() ?: it.name()) == tableName }
    }

    override fun visit(table: Table) {
        tableDefinitions +=
            PhysicalTableDefinition(
                catalogName = table.database.databaseName,
                schemaName = table.schemaName,
                tableName = table.name,
                alias = table.alias?.name,
            )
    }

    override fun visit(subSelect: SubSelect) {
        // If the FROM clause contains a subquery, we need to process it as well
        val subSelectVisitor = LineagePlainSelectVisitor()
        subSelect.selectBody.accept(subSelectVisitor)

        // Add the subquery's table sources to the main table sources
        tableDefinitions +=
            DerivedTableDefinition(
                columns = subSelectVisitor.selectItems.flatMap { it.sourceColumns }.toSet(),
                references = subSelectVisitor.referencedColumns,
                alias = checkNotNull(subSelect.alias?.name) { "Subquery must have an alias" },
            )
    }
}
