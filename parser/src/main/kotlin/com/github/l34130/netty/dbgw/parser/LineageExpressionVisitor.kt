package com.github.l34130.netty.dbgw.parser

import net.sf.jsqlparser.expression.ExpressionVisitorAdapter
import net.sf.jsqlparser.schema.Column
import net.sf.jsqlparser.statement.select.Select

class LineageExpressionVisitor : ExpressionVisitorAdapter<Unit>() {
    override fun <S : Any?> visit(
        column: Column,
        context: S?,
    ) {
        val ctx = (context as LineageContext)
        val tableSource = ctx.resolveTable(column.table?.name)

        if (tableSource != null) {
            ctx.referencedColumns += tableSource.getOriginalColumnSource(column.columnName)
        } else {
            ctx.referencedColumns += DelayedColumnRef(ctx.tableSources, column.columnName)
        }
    }

    override fun <S : Any?> visit(
        select: Select,
        context: S?,
    ) {
        val ctx = (context as LineageContext)
        val childCtx = ctx.childContext()

        // If the WHERE clause contains a subquery, we need to process it as well
        val subSelectVisitor = LineagePlainSelectVisitor()
        select.accept(subSelectVisitor, childCtx)

        ctx.referencedColumns += childCtx.selectItems.flatMap { it.sourceColumns }.toSet()
        ctx.referencedColumns += childCtx.referencedColumns
    }
}
