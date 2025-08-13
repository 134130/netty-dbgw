
package com.github.l34130.netty.dbgw.parser

import net.sf.jsqlparser.schema.Table
import net.sf.jsqlparser.statement.select.FromItemVisitorAdapter
import net.sf.jsqlparser.statement.select.LateralSubSelect
import net.sf.jsqlparser.statement.select.ParenthesedSelect
import net.sf.jsqlparser.statement.select.PlainSelect

class LineageFromItemVisitor : FromItemVisitorAdapter<Unit>() {
    override fun <S : Any?> visit(
        table: Table,
        context: S?,
    ) {
        val ctx = (context as LineageContext)
        ctx.tableSources +=
            PhysicalTableDefinition(
                catalogName = table.database.databaseName,
                schemaName = table.schemaName,
                tableName = table.name,
                alias = table.alias?.name,
            )
    }

    override fun <S : Any?> visit(
        lateralSubSelect: LateralSubSelect,
        context: S?,
    ) = super.visit(lateralSubSelect, context)

    override fun <S : Any?> visit(
        plainSelect: PlainSelect,
        context: S?,
    ) = super.visit(plainSelect, context)

    override fun <S : Any?> visit(
        select: ParenthesedSelect,
        context: S?,
    ) {
        val ctx = (context as LineageContext)
        val childCtx = ctx.childContext()

        // If the FROM clause contains a subquery, we need to process it as well
        val subSelectVisitor = LineagePlainSelectVisitor()
        select.select.accept(subSelectVisitor, childCtx)

        // Add the subquery table source to the main table sources
        ctx.tableSources +=
            DerivedTableDefinition(
                columns = childCtx.selectItems.flatMap { it.sourceColumns }.toSet(),
                references = childCtx.referencedColumns,
                // NOTE: DO we need to trim the alias? (is it the jsqlparser's bug?)
                alias = checkNotNull(select.alias?.name?.trim()) { "Subquery must have an alias" },
            )
        ctx.referencedColumns += childCtx.referencedColumns
    }
}
