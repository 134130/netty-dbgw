package com.github.l34130.netty.dbgw.policy.builtin.database

import com.github.l34130.netty.dbgw.policy.api.PolicyDecision
import com.github.l34130.netty.dbgw.policy.api.PolicyDefinition
import com.github.l34130.netty.dbgw.policy.api.database.DatabasePolicy
import com.github.l34130.netty.dbgw.policy.api.database.DatabaseQueryPolicyContext

class DatabaseStatementTypePolicy(
    private val definition: DatabaseStatementTypePolicyDefinition,
    val statements: List<String>,
    val action: DatabaseStatementTypePolicyDefinition.Action,
) : DatabasePolicy {
    override fun definition(): PolicyDefinition = definition

    override fun onQuery(ctx: DatabaseQueryPolicyContext) {
        // TODO: Parse the query to extract the statement type
        //  For now, we will just check if the query contains any of the statements
        for (stmt in statements) {
            if (ctx.query.contains(stmt, ignoreCase = true)) {
                ctx.decision =
                    when (action) {
                        DatabaseStatementTypePolicyDefinition.Action.ALLOW ->
                            PolicyDecision.Allow(
                                reason = "Allowed statement: $stmt",
                            )
                        DatabaseStatementTypePolicyDefinition.Action.DENY ->
                            PolicyDecision.Deny(
                                reason = "Disallowed statement: $stmt",
                            )
                    }

                return
            }
        }

        ctx.decision = PolicyDecision.NotApplicable
    }
}
