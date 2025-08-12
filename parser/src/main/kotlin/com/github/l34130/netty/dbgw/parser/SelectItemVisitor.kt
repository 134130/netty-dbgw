package com.github.l34130.netty.dbgw.parser

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
//            }

        val expressionVisitor = ExpressionVisitor(fromItemVisitor)
        expression.accept(expressionVisitor)
        columnRefs += expressionVisitor.columnRefs
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
