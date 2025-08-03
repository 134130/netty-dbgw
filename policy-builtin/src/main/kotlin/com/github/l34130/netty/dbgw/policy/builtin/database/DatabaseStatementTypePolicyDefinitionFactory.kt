package com.github.l34130.netty.dbgw.policy.builtin.database

import com.github.l34130.netty.dbgw.policy.api.config.AbstractResourceFactory

class DatabaseStatementTypePolicyDefinitionFactory :
    AbstractResourceFactory<DatabaseStatementTypePolicyDefinition>(DatabaseStatementTypePolicyDefinition::class) {
    override fun create(props: Map<String, Any>): DatabaseStatementTypePolicyDefinition =
        DatabaseStatementTypePolicyDefinition(
            statements = props["statements"] as List<String>,
            action = DatabaseStatementTypePolicyDefinition.Action.valueOf(props["action"] as String),
        )
}
