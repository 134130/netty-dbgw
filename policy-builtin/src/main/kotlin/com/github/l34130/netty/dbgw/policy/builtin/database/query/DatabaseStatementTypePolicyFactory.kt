package com.github.l34130.netty.dbgw.policy.builtin.database.query

import com.github.l34130.netty.dbgw.policy.api.config.AbstractResourceFactory

class DatabaseStatementTypePolicyFactory : AbstractResourceFactory<DatabaseStatementTypePolicy>(DatabaseStatementTypePolicy::class) {
    override fun create(props: Map<String, Any>): DatabaseStatementTypePolicy =
        DatabaseStatementTypePolicy(
            statements = props["statements"] as List<String>,
            action = DatabaseStatementTypePolicy.Action.valueOf(props["action"] as String),
        )
}
