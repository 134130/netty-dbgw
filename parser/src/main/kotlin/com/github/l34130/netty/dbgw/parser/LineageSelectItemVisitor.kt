package com.github.l34130.netty.dbgw.parser

import net.sf.jsqlparser.expression.ExpressionVisitorAdapter
import net.sf.jsqlparser.expression.Function
import net.sf.jsqlparser.schema.Column
import net.sf.jsqlparser.statement.select.AllColumns
import net.sf.jsqlparser.statement.select.AllTableColumns
import net.sf.jsqlparser.statement.select.SelectExpressionItem
import net.sf.jsqlparser.statement.select.SelectItemVisitorAdapter

class LineageSelectItemVisitor(
    private val fromItemVisitor: LineageFromItemVisitor,
) : SelectItemVisitorAdapter() {
    val selectItems = mutableSetOf<SelectItem>()

    override fun visit(selectExpressionItem: SelectExpressionItem) {
        val expression = selectExpressionItem.expression
        val alias = selectExpressionItem.alias?.name

        val itemVisitor =
            object : ExpressionVisitorAdapter() {
                override fun visit(function: Function) {
                    val functionParamVisitor = LineageExpressionVisitor(fromItemVisitor)
                    function.parameters?.expressions?.forEach { expr ->
                        expr.accept(functionParamVisitor)
                    }

                    selectItems +=
                        FunctionColumn(
                            functionName = function.name,
                            arguments = function.parameters?.expressions?.map { it.toString() } ?: emptyList(),
                            sourceColumns = functionParamVisitor.columnRefs,
                            alias = alias,
                        )
                }

                override fun visit(column: Column) {
                    val tableSource =
                        fromItemVisitor.resolveTable(column.table?.name)
                            ?: error("Column '${column.columnName}' does not exist in the FROM clause.")
                    val columnRef = tableSource.getOriginalColumnSource(column.columnName)
                    selectItems += DirectColumn(columnRef, customAlias = alias)
                }
            }
        expression.accept(itemVisitor)
    }

    override fun visit(allColumns: AllColumns) {
        if (fromItemVisitor.tableDefinitions.size != 1) {
            error(
                "AllColumns can only be used when there is exactly one table in the FROM clause. Found: ${fromItemVisitor.tableDefinitions.size} tables.",
            )
        }
        val tableSource = fromItemVisitor.tableDefinitions.first()
        val columnRef =
            ColumnRef(
                tableSource = tableSource,
                columnName = "*",
            )
        selectItems += DirectColumn(columnRef)
    }

    override fun visit(allTableColumns: AllTableColumns) {
        val tableName = allTableColumns.table.name
        val tableSource =
            fromItemVisitor.tableDefinitions.find { it.alias() == tableName }
                ?: error("Table '$tableName' does not exist in the FROM clause.")

        val columnRef =
            ColumnRef(
                tableSource = tableSource,
                columnName = "*",
            )
        selectItems += DirectColumn(columnRef)
    }
}
