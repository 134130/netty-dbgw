package com.github.l34130.netty.dbgw.parser

import net.sf.jsqlparser.statement.StatementVisitorAdapter
import net.sf.jsqlparser.statement.select.Select

class LineageStatementVisitor : StatementVisitorAdapter() {
    private val selectVisitor = LineageSelectVisitor()

    val selectItems: Set<SelectItem>
        get() = selectVisitor.selectItems
    val referencedColumns: Set<ColumnRef>
        get() = selectVisitor.referencedColumns

    override fun visit(select: Select) {
        select.selectBody.accept(selectVisitor)
    }
}
