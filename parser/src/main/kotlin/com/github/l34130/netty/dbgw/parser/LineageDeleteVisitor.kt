package com.github.l34130.netty.dbgw.parser

import net.sf.jsqlparser.statement.StatementVisitorAdapter
import net.sf.jsqlparser.statement.delete.Delete

class LineageDeleteVisitor : StatementVisitorAdapter() {
    private val fromItemVisitor = LineageFromItemVisitor()
    private val joinItemVisitor = LineageFromItemVisitor()
    private val expressionVisitor =
        LineageExpressionVisitor { fromItemVisitor.tableDefinitions + joinItemVisitor.tableDefinitions }

    val selectItems: Set<SelectItem>
        get() = fromItemVisitor.tableDefinitions.map { DirectColumn(ColumnRef(it, "*")) }.toSet()
    val referencedColumns: Set<ColumnRef>
        get() = expressionVisitor.columnRefs

    override fun visit(delete: Delete) {
        /**
         * DELETE FROM orders
         * JOIN customers ON orders.customer_id = customers.id
         * WHERE customers.is_vip = false
         * ORDER BY order_date ASC
         * LIMIT 100;
         */

        delete.table?.accept(fromItemVisitor)
        delete.tables?.forEach { it.accept(fromItemVisitor) }
        delete.joins?.forEach {
            it.rightItem.accept(joinItemVisitor)
            it.onExpression.accept(expressionVisitor)
        }
        delete.where?.accept(expressionVisitor)
        delete.orderByElements?.forEach {
            TODO()
        }
    }
}
