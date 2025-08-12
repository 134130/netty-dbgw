package com.github.l34130.netty.dbgw.parser

import com.github.l34130.netty.dbgw.common.database.ColumnDefinition

interface ColumnTracker<T> {
    fun parse(stmt: T): List<ColumnDefinition>
}
