package com.github.l34130.netty.dbgw.policy.builtin.database

import com.github.l34130.netty.dbgw.policy.api.config.AbstractResourceFactory

class DatabaseRowLevelSecurityPolicyDefinitionFactory :
    AbstractResourceFactory<DatabaseRowLevelSecurityPolicyDefinition>(DatabaseRowLevelSecurityPolicyDefinition::class) {
    override fun create(props: Map<String, Any>): DatabaseRowLevelSecurityPolicyDefinition =
        DatabaseRowLevelSecurityPolicyDefinition(
            database = props["database"] as String?,
            schema = props["schema"] as String?,
            table = props["table"] as String?,
            column = props["column"] as String,
            filterRegex = props["filterRegex"] as String,
            action =
                DatabaseRowLevelSecurityPolicyDefinition.Action.valueOf(
                    props["action"] as String,
                ),
        )
}
