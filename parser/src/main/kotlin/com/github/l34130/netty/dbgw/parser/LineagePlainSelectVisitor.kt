package com.github.l34130.netty.dbgw.parser

import net.sf.jsqlparser.statement.select.PlainSelect
import net.sf.jsqlparser.statement.select.SelectVisitorAdapter

class LineagePlainSelectVisitor : SelectVisitorAdapter<Unit>() {
    private val fromItemVisitor = LineageFromItemVisitor()
    private val expressionVisitor = LineageExpressionVisitor()
    private val selectItemVisitor = LineageSelectItemVisitor()
    private val orderByVisitor = LineageOrderByVisitor()

    override fun <S : Any?> visit(
        plainSelect: PlainSelect,
        context: S?,
    ) {
        plainSelect.fromItem.accept(fromItemVisitor, context)
        plainSelect.joins?.forEach { join ->
            join.rightItem.accept(fromItemVisitor, context)
            join.onExpressions?.forEach { it.accept(expressionVisitor, context) }
            join.usingColumns?.forEach { it.accept(expressionVisitor, context) }
        }
        plainSelect.where?.accept(expressionVisitor, context)
        plainSelect.selectItems.forEach { selectItem ->
            selectItem.accept(selectItemVisitor, context)
        }
        plainSelect.groupBy?.let {
            TODO()
        }
        plainSelect.orderByElements?.forEach {
            orderByVisitor.visit(it, context)
        }
        plainSelect.distinct?.let {
            TODO()
        }
        plainSelect.intoTables?.let {
            TODO()
        }
    }
}
