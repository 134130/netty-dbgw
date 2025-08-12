package com.github.l34130.netty.dbgw.parser

import net.sf.jsqlparser.expression.ExpressionVisitorAdapter
import net.sf.jsqlparser.schema.Column
import net.sf.jsqlparser.statement.select.SubSelect

class ExpressionVisitor(
    val fromItemVisitor: FromItemVisitor,
) : ExpressionVisitorAdapter() {
    val columnRefs = mutableSetOf<ColumnRef>()

    override fun visit(column: Column) {
        val tableName = column.table?.name // In where clause, the table alias may not be present
        val columnName = column.columnName

        if (fromItemVisitor.tableDefinitions.size == 1) {
            // If there's only one table, we can assume the column belongs to that table
            val tableSource = fromItemVisitor.tableDefinitions.first()
            columnRefs += tableSource.getOriginalColumnSource(column.columnName)
            return
        }

        // If there are multiple tables, we need to find the correct table source
        val tableSource =
            fromItemVisitor.tableDefinitions.find { it.alias() == tableName }
                ?: error("Column '$columnName' refers to a table alias '$tableName' that does not exist in the FROM clause.")

        columnRefs +=
            ColumnRef(
                tableSource = tableSource,
                columnName = columnName,
            )
    }

    override fun visit(subSelect: SubSelect) {
        // If the WHERE clause contains a subquery, we need to process it as well
        val subSelectVisitor = LineagePlainSelectVisitor()
        subSelect.selectBody.accept(subSelectVisitor)

        columnRefs += subSelectVisitor.columns
        columnRefs += subSelectVisitor.referencedColumns
    }
}
