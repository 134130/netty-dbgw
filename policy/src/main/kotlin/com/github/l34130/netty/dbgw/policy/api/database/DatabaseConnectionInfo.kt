package com.github.l34130.netty.dbgw.policy.api.database

data class DatabaseConnectionInfo(
    /**
     * [com.github.l34130.netty.dbgw.common.DatabaseType.id]
     */
    var databaseType: String,
    var database: String? = null,
    var schema: String? = null,
)
