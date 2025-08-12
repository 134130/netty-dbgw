package com.github.l34130.netty.dbgw.parser

import net.sf.jsqlparser.statement.select.PlainSelect
import net.sf.jsqlparser.statement.select.SelectVisitorAdapter
import net.sf.jsqlparser.statement.select.SetOperationList
import net.sf.jsqlparser.statement.select.WithItem
import net.sf.jsqlparser.statement.values.ValuesStatement

class LineageSelectVisitor : SelectVisitorAdapter() {
    private val plainSelectVisitor = LineagePlainSelectVisitor()

    val selectItems: Set<SelectItem>
        get() = plainSelectVisitor.selectItems
    val referencedColumns: Set<ColumnRef>
        get() = plainSelectVisitor.referencedColumns

    override fun visit(plainSelect: PlainSelect) {
        plainSelect.accept(plainSelectVisitor)
    }

    override fun visit(aThis: ValuesStatement?) {
        TODO("not implemented")
    }

    override fun visit(setOpList: SetOperationList?) {
        TODO()
    }

    override fun visit(withItem: WithItem?) {
        TODO()
    }
}
