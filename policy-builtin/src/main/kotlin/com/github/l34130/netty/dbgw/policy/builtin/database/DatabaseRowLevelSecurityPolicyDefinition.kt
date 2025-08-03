package com.github.l34130.netty.dbgw.policy.builtin.database

import com.github.l34130.netty.dbgw.policy.api.PolicyDefinition
import com.github.l34130.netty.dbgw.policy.api.config.Resource
import com.github.l34130.netty.dbgw.policy.api.database.DatabasePolicy

@Resource(
    group = "builtin",
    version = "v1",
    kind = "DatabaseRowLevelSecurityPolicy",
    plural = "databaserowlevelsecuritypolicies",
    singular = "databaserowlevelsecuritypolicy",
)
data class DatabaseRowLevelSecurityPolicyDefinition(
    val database: String?,
    val schema: String?,
    val table: String?,
    val column: String,
    val filterRegex: String,
    val action: Action,
) : PolicyDefinition {
    override fun createPolicy(): DatabasePolicy =
        DatabaseRowLevelSecurityPolicy(
            definition = this,
            database = database,
            schema = schema,
            table = table,
            column = column,
            filterRegex = filterRegex,
            action = action,
        )

    enum class Action { ALLOW, DENY }
}
