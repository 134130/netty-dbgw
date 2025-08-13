package com.github.l34130.netty.dbgw.parser

import net.sf.jsqlparser.statement.select.OrderByElement
import net.sf.jsqlparser.statement.select.OrderByVisitorAdapter

class LineageOrderByVisitor : OrderByVisitorAdapter<Unit>() {
    override fun <S : Any?> visit(
        orderBy: OrderByElement,
        context: S?,
    ) {
        val ctx = (context as LineageContext)
        val expressionVisitor = LineageExpressionVisitor()
        orderBy.expression.accept(expressionVisitor, ctx)
    }
}
