package com.github.l34130.netty.dbgw.parser

import net.sf.jsqlparser.statement.StatementVisitorAdapter
import net.sf.jsqlparser.statement.delete.Delete

class LineageDeleteVisitor : StatementVisitorAdapter<Unit>() {
    private val fromItemVisitor = LineageFromItemVisitor()
    private val joinItemVisitor = LineageFromItemVisitor()
    private val expressionVisitor = LineageExpressionVisitor()
    private val orderByVisitor = LineageOrderByVisitor()

    override fun <S : Any?> visit(
        delete: Delete,
        context: S?,
    ) {
        /**
         * DELETE FROM orders
         * JOIN customers ON orders.customer_id = customers.id
         * WHERE customers.is_vip = false
         * ORDER BY order_date ASC
         * LIMIT 100;
         */
        val ctx = context as LineageContext

        delete.table?.accept(fromItemVisitor, ctx)
        delete.tables?.forEach { it.accept(fromItemVisitor, ctx) }

        ctx.selectItems +=
            ctx.tableSources.map {
                DirectColumn(
                    columnRef =
                        DirectColumnRef(
                            tableSource =
                                PhysicalTableDefinition(
                                    tableName = it.name(),
                                    alias = it.alias(),
                                ),
                            columnName = "*",
                        ),
                    customAlias = it.alias(),
                )
            }

        delete.joins?.forEach {
            it.rightItem.accept(joinItemVisitor, ctx)
            it.onExpressions?.forEach { it.accept(expressionVisitor, ctx) }
        }
        delete.where?.accept(expressionVisitor, ctx)
        delete.orderByElements?.forEach {
            it.accept(orderByVisitor, ctx)
        }
    }
}
