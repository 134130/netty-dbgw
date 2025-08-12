package com.github.l34130.netty.dbgw.parser

import net.sf.jsqlparser.expression.ExpressionVisitorAdapter
import net.sf.jsqlparser.schema.Column
import net.sf.jsqlparser.statement.select.AllColumns
import net.sf.jsqlparser.statement.select.AllTableColumns
import net.sf.jsqlparser.statement.select.SelectExpressionItem
import net.sf.jsqlparser.statement.select.SelectItemVisitorAdapter

class SelectItemVisitor(
    private val fromItemVisitor: FromItemVisitor,
) : SelectItemVisitorAdapter() {
    val columnRefs = mutableSetOf<ColumnRef>()

    override fun visit(selectExpressionItem: SelectExpressionItem) {
        val expression = selectExpressionItem.expression
        val alias = selectExpressionItem.alias

        val expressionVisitor =
            object : ExpressionVisitorAdapter() {
                override fun visit(column: Column) {
                    if (fromItemVisitor.tableDefinitions.size == 1) {
                        // If there's only one table, we can assume the column belongs to that table
                        val tableSource = fromItemVisitor.tableDefinitions.first()
                        columnRefs += tableSource.getOriginalColumnSource(column.columnName)
                        return
                    }

                    // If there are multiple tables, we need to find the correct table source
                    val tableSource =
                        fromItemVisitor.tableDefinitions.find { it.alias() == column.table?.name }
                            ?: error(
                                "Column '${column.columnName}' refers to a table alias '${column.table?.name}' that does not exist in the FROM clause.",
                            )

                    columnRefs += tableSource.getOriginalColumnSource(column.columnName)
                }

//                override fun visit(function: Function) {
//                    val functionParamVisitor = WhereClauseColumnVisitor(tableVisitor)
//                    function.parameters?.expressions?.forEach { expr ->
//                        expr.accept(functionParamVisitor)
//                    }
//
//                    val aliasName = alias?.name ?: function.name
//                    columnRefs +=
//                        ColumnRef(
//                            tableSource =
//                                DerivedTableDefinition(
//                                    columns = emptySet(),
//                                    references = functionParamVisitor.columnRefs,
//                                    alias = "", // function output does not have a table alias
//                                ),
//                            columnName = aliasName,
//                        )
//                }
            }
        expression.accept(expressionVisitor)
    }

    override fun visit(allColumns: AllColumns) {
        if (fromItemVisitor.tableDefinitions.size == 1) {
            // If there's only one table, we can assume all columns belong to that table
            val tableSource = fromItemVisitor.tableDefinitions.first()
            columnRefs +=
                ColumnRef(
                    tableSource = tableSource,
                    columnName = "*", // Represents all columns from the table
                )
            return
        }

        error(
            "AllColumns can only be used when there is exactly one table in the FROM clause. Found: ${fromItemVisitor.tableDefinitions.size} tables.",
        )
    }

    override fun visit(allTableColumns: AllTableColumns) {
        val tableName = allTableColumns.table.name
        val tableSource =
            fromItemVisitor.tableDefinitions.find { it.alias() == tableName }
                ?: error("Table '$tableName' does not exist in the FROM clause.")

        columnRefs +=
            ColumnRef(
                tableSource = tableSource,
                columnName = "*", // Represents all columns from the specified table
            )
    }
}
