package com.github.l34130.netty.dbgw.parser

import com.github.l34130.netty.dbgw.common.database.ColumnDefinition
import net.sf.jsqlparser.statement.select.PlainSelect

class MyPlainSelectColumnTracker : ColumnTracker<PlainSelect> {
    override fun parse(stmt: PlainSelect): List<ColumnDefinition> {
        // This is a placeholder implementation.
        // In a real implementation, you would parse the PlainSelect statement
        // and extract the column definitions.
        return emptyList()
    }

//    fun parse2(stmt: PlainSelect): List<ColumnGraphNode>
}
