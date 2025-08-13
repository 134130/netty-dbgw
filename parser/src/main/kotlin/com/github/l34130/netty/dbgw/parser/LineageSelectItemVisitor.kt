package com.github.l34130.netty.dbgw.parser

import net.sf.jsqlparser.expression.Expression
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter
import net.sf.jsqlparser.expression.Function
import net.sf.jsqlparser.schema.Column
import net.sf.jsqlparser.statement.select.AllColumns
import net.sf.jsqlparser.statement.select.AllTableColumns
import net.sf.jsqlparser.statement.select.SelectItemVisitorAdapter

class LineageSelectItemVisitor : SelectItemVisitorAdapter<Unit>() {
    override fun <S : Any?> visit(
        item: net.sf.jsqlparser.statement.select.SelectItem<out Expression>,
        context: S?,
    ) {
        val expression = item.expression
        val alias = item.alias?.name

        val itemVisitor =
            object : ExpressionVisitorAdapter<Unit>() {
                override fun <S : Any?> visit(
                    function: Function,
                    context: S?,
                ) {
                    val ctx = (context as LineageContext)
                    val childCtx =
                        ctx.childContext().apply {
                            tableSources += ctx.tableSources
                        }
                    val functionParamVisitor = LineageExpressionVisitor()
                    function.parameters?.accept(functionParamVisitor, childCtx)

                    ctx.selectItems +=
                        FunctionColumn(
                            functionName = function.name,
                            arguments = function.parameters?.map { it.toString() } ?: emptyList(),
                            sourceColumns = childCtx.referencedColumns,
                            alias = alias,
                        )
                }

                override fun <S : Any?> visit(
                    column: Column,
                    context: S?,
                ) {
                    val ctx = (context as LineageContext)
                    val tableSource =
                        ctx.resolveTable(column.table?.name)
                            ?: error("Column '${column.columnName}' does not exist in the FROM clause.")
                    val columnRef = tableSource.getOriginalColumnSource(column.columnName)
                    ctx.selectItems += DirectColumn(columnRef, customAlias = alias)
                }

                override fun <S : Any?> visit(
                    allColumns: AllColumns,
                    context: S?,
                ) {
                    val ctx = (context as LineageContext)
                    if (ctx.tableSources.size != 1) {
                        error(
                            "AllColumns can only be used when there is exactly one table in the FROM clause. Found: ${ctx.tableSources.size} tables.",
                        )
                    }
                    val tableSource = ctx.tableSources.first()
                    val columnRef =
                        DirectColumnRef(
                            tableSource = tableSource,
                            columnName = "*",
                        )
                    ctx.selectItems += DirectColumn(columnRef)
                }

                override fun <S : Any?> visit(
                    allTableColumns: AllTableColumns,
                    context: S?,
                ) {
                    val ctx = (context as LineageContext)
                    val tableName = allTableColumns.table.name
                    val tableSource = ctx.resolveTable(tableName) ?: error("Table '$tableName' does not exist in the FROM clause.")

                    val columnRef =
                        DirectColumnRef(
                            tableSource = tableSource,
                            columnName = "*",
                        )
                    ctx.selectItems += DirectColumn(columnRef)
                }
            }

        expression?.accept(itemVisitor, context)
    }
}
