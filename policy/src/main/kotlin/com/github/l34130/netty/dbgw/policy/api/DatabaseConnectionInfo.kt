package com.github.l34130.netty.dbgw.policy.api

data class DatabaseConnectionInfo(
    /**
     * [com.github.l34130.netty.dbgw.common.DatabaseType.id]
     */
    val databaseType: String,
    val database: String? = null,
    val schema: String? = null,
)
