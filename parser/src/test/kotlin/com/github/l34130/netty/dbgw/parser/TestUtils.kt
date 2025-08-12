package com.github.l34130.netty.dbgw.parser

import java.util.concurrent.ConcurrentHashMap

object TestUtils {
    private val tables = ConcurrentHashMap<String, PhysicalTableDefinition>()
    private val columns = ConcurrentHashMap<String, ConcurrentHashMap<String, ColumnRef>>()

    fun column(
        table: String,
        column: String,
        tableAlias: String? = null,
        columnAlias: String? = null,
    ): ColumnRef {
        val tableKey = "$table:$tableAlias"
        return columns
            .computeIfAbsent(tableKey) {
                tables.computeIfAbsent(tableKey) {
                    PhysicalTableDefinition(tableName = table, alias = tableAlias)
                }
                ConcurrentHashMap()
            }.computeIfAbsent(column) {
                ColumnRef(tables[tableKey]!!, column)
            }
    }
}
