package com.github.l34130.netty.dbgw.policy.builtin.database.query

import com.github.l34130.netty.dbgw.policy.api.Policy
import com.github.l34130.netty.dbgw.policy.api.query.AbstractDatabaseQueryPolicy
import com.github.l34130.netty.dbgw.policy.api.query.DatabaseQueryPolicyContext
import com.github.l34130.netty.dbgw.policy.api.query.DatabaseQueryPolicyResult

@Policy(
    group = "builtin",
    version = "v1",
    kind = "DatabaseQueryStatementType",
    plural = "databasestatementtypepolicies",
    singular = "databasestatementtypepolicy",
)
data class DatabaseStatementTypePolicy(
    val statements: List<String>,
    val action: Action,
) : AbstractDatabaseQueryPolicy() {
    override fun evaluate(
        ctx: DatabaseQueryPolicyContext,
        query: String,
    ): DatabaseQueryPolicyResult {
        // TODO: Parse the query to extract the statement type
        //  For now, we will just check if the query contains any of the statements
        for (stmt in statements) {
            if (query.contains(stmt, ignoreCase = true)) {
                return when (action) {
                    Action.ALLOW ->
                        DatabaseQueryPolicyResult.Allowed(
                            reason = "Allowed statement: $stmt",
                        )
                    Action.DENY ->
                        DatabaseQueryPolicyResult.Denied(
                            reason = "Disallowed statement: $stmt",
                        )
                }
            }
        }

        return when (action) {
            Action.ALLOW -> DatabaseQueryPolicyResult.Denied(reason = "No allowed statements found")
            Action.DENY -> DatabaseQueryPolicyResult.Allowed(reason = "No disallowed statements found")
        }
    }

    enum class Action { ALLOW, DENY }
}
