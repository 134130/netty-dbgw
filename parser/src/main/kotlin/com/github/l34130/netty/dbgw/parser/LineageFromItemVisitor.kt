
package com.github.l34130.netty.dbgw.parser

import net.sf.jsqlparser.schema.Table
import net.sf.jsqlparser.statement.select.FromItemVisitorAdapter
import net.sf.jsqlparser.statement.select.SubSelect

class LineageFromItemVisitor : FromItemVisitorAdapter() {
    val tableDefinitions: MutableSet<TableDefinition> = mutableSetOf()

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
