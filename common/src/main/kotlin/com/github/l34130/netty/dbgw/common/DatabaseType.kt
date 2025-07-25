package com.github.l34130.netty.dbgw.common

enum class DatabaseType(
    val id: String,
    val displayName: String,
) {
    MYSQL("mysql", "MySQL"),
    POSTGRES("postgres", "PostgreSQL"),
}
