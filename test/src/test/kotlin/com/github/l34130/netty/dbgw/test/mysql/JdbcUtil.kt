package com.github.l34130.netty.dbgw.test.mysql

import java.sql.Connection

fun Connection.executeQuery(query: String): List<List<Any?>> {
    val result = mutableListOf<List<Any?>>()
    this.createStatement().use { stmt ->
        stmt.executeQuery(query).use { rs ->
            val meta = rs.metaData
            result.add((1..meta.columnCount).map { i -> meta.getColumnLabel(i) })
            while (rs.next()) {
                val row = mutableListOf<Any?>()
                for (i in 1..meta.columnCount) {
                    row.add(rs.getObject(i))
                }
                result.add(row.toList())
            }
        }
    }
    return result
}
