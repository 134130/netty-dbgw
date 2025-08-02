package com.github.l34130.netty.dbgw.policy.builtin.database.query

import com.github.l34130.netty.dbgw.policy.api.PolicyDecision
import com.github.l34130.netty.dbgw.policy.api.config.Resource
import com.github.l34130.netty.dbgw.policy.api.database.DatabasePolicyInterceptor
import com.github.l34130.netty.dbgw.policy.api.database.query.DatabaseQueryContext

@Resource(
    group = "builtin",
    version = "v1",
    kind = "DatabaseStatementTypePolicy",
    plural = "databasestatementtypepolicies",
    singular = "databasestatementtypepolicy",
)
data class DatabaseStatementTypePolicy(
    val statements: List<String>,
    val action: Action,
) : DatabasePolicyInterceptor {
    override fun onQuery(ctx: DatabaseQueryContext): PolicyDecision {
        // TODO: Parse the query to extract the statement type
        //  For now, we will just check if the query contains any of the statements
        for (stmt in statements) {
            if (ctx.query.contains(stmt, ignoreCase = true)) {
                return when (action) {
                    Action.ALLOW ->
                        PolicyDecision.Allow(
                            reason = "Allowed statement: $stmt",
                        )
                    Action.DENY ->
                        PolicyDecision.Deny(
                            reason = "Disallowed statement: $stmt",
                        )
                }
            }
        }

        return PolicyDecision.NotApplicable
    }

    enum class Action { ALLOW, DENY }
}
