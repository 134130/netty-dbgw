package com.github.l34130.netty.dbgw.parser

import com.github.l34130.netty.dbgw.common.database.ColumnDefinition

interface SqlParser {
    fun parse(sql: String): List<ColumnDefinition>
}
