package com.github.l34130.netty.dbgw.test.mysql

import java.sql.Connection
import java.sql.ResultSet

fun Connection.executeQuery(query: String): List<List<Any?>> =
    this.createStatement().use { stmt ->
        stmt.executeQuery(query).use { rs ->
            rs.readAsTable()
        }
    }

fun ResultSet.readAsTable(): List<List<Any?>> {
    val result = mutableListOf<List<Any?>>()
    val meta = this.metaData
    result.add((1..meta.columnCount).map { i -> meta.getColumnLabel(i) })
    while (this.next()) {
        val row = mutableListOf<Any?>()
        for (i in 1..meta.columnCount) {
            row.add(this.getObject(i))
        }
        result.add(row.toList())
    }
    return result
}
