package com.github.l34130.netty.dbgw.policy.builtin.database.query

import com.github.l34130.netty.dbgw.policy.api.PolicyDefinition
import com.github.l34130.netty.dbgw.policy.api.config.Resource
import com.github.l34130.netty.dbgw.policy.api.database.DatabasePolicyInterceptor

@Resource(
    group = "builtin",
    version = "v1",
    kind = "DatabaseStatementTypePolicy",
    plural = "databasestatementtypepolicies",
    singular = "databasestatementtypepolicy",
)
data class DatabaseStatementTypePolicyDefinition(
    val statements: List<String>,
    val action: Action,
) : PolicyDefinition {
    override fun createInterceptor(): DatabasePolicyInterceptor =
        DatabaseStatementTypePolicy(
            statements = statements,
            action = action,
        )

    enum class Action { ALLOW, DENY }
}
