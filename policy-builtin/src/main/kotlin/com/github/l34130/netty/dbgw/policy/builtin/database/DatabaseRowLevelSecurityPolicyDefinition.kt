package com.github.l34130.netty.dbgw.policy.builtin.database

import com.github.l34130.netty.dbgw.common.database.FullyQualifiedName
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
    /**
     * The catalog name to which this policy applies.
     * Defaults to `.*` which matches all catalogs.
     * This supports regex patterns to match multiple columns.
     */
    val catalog: String = ".*",
    /**
     * The schema name to which this policy applies.
     * Defaults to `.*` which matches all schemas.
     * This supports regex patterns to match multiple columns.
     */
    val schema: String = ".*",
    /**
     * The table name to which this policy applies.
     * Defaults to `.*` which matches all tables.
     * This supports regex patterns to match multiple columns.
     */
    val table: String = ".*",
    /**
     * The column name to which this policy applies.
     * Defaults to `.*` which matches all columns.
     * This supports regex patterns to match multiple columns.
     */
    val column: String = ".*",
    /**
     * The filter expression that defines the row-level security condition.
     * This should be a regex pattern that matches the data in the specified column.
     */
    val filter: String,
    /**
     * The action to take when the filter matches.
     */
    val action: Action,
) : PolicyDefinition {
    override fun createPolicy(): DatabasePolicy =
        DatabaseRowLevelSecurityPolicy(
            definition = this,
            fqn =
                FullyQualifiedName(
                    catalog = catalog,
                    schema = schema,
                    table = table,
                    column = column,
                ),
            filter = filter,
            action = action,
        )

    enum class Action { ALLOW, DENY }
}
