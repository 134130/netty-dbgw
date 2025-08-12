package com.github.l34130.netty.dbgw.parser

import net.sf.jsqlparser.statement.select.PlainSelect
import net.sf.jsqlparser.statement.select.SelectVisitorAdapter

class LineagePlainSelectVisitor : SelectVisitorAdapter() {
    private val fromItemVisitor = FromItemVisitor()
    private val expressionVisitor = ExpressionVisitor(fromItemVisitor)
    private val selectItemVisitor = SelectItemVisitor(fromItemVisitor)

    val columns: Set<ColumnRef>
        get() = selectItemVisitor.columnRefs
    val referencedColumns: Set<ColumnRef>
        get() {
            val fromWhere = expressionVisitor.columnRefs
            val fromSubQueries = fromItemVisitor.tableDefinitions.flatMap { it.getAllReferences() }
            return fromWhere + fromSubQueries
        }

    override fun visit(plainSelect: PlainSelect) {
        plainSelect.fromItem.accept(fromItemVisitor)
        plainSelect.joins?.forEach { join ->
            join.rightItem.accept(fromItemVisitor)
            join.onExpression?.accept(expressionVisitor)
        }
        plainSelect.where?.accept(expressionVisitor)
        plainSelect.selectItems.forEach { selectItem ->
            selectItem.accept(selectItemVisitor)
        }

        plainSelect.distinct?.let {
            TODO()
        }

        plainSelect.intoTables?.let {
            TODO()
        }

        plainSelect.orderByElements?.let {
            TODO()
        }
    }
}
