package com.github.l34130.netty.dbgw.parser

import net.sf.jsqlparser.expression.ExpressionVisitorAdapter
import net.sf.jsqlparser.schema.Column
import net.sf.jsqlparser.statement.select.SubSelect

class LineageExpressionVisitor(
    val tableSourceProvider: TableSourceProvider,
) : ExpressionVisitorAdapter() {
    val columnRefs = mutableSetOf<ColumnRef>()

    override fun visit(column: Column) {
        val tableSource =
            tableSourceProvider.resolveTable(column.table?.name)
                ?: error("Column '${column.columnName}' refers to a table '${column.table?.name}' that does not exist in the FROM clause.")
        columnRefs += tableSource.getOriginalColumnSource(column.columnName)
    }

    override fun visit(subSelect: SubSelect) {
        // If the WHERE clause contains a subquery, we need to process it as well
        val subSelectVisitor = LineagePlainSelectVisitor()
        subSelect.selectBody.accept(subSelectVisitor)

        columnRefs += subSelectVisitor.selectItems.flatMap { it.sourceColumns }
        columnRefs += subSelectVisitor.referencedColumns
    }
}
