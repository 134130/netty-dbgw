package com.github.l34130.netty.dbgw.policy.builtin.database

import com.github.l34130.netty.dbgw.policy.api.config.AbstractResourceFactory

class DatabaseRowLevelSecurityPolicyDefinitionFactory :
    AbstractResourceFactory<DatabaseRowLevelSecurityPolicyDefinition>(DatabaseRowLevelSecurityPolicyDefinition::class) {
    override fun create(props: Map<String, Any>): DatabaseRowLevelSecurityPolicyDefinition =
        DatabaseRowLevelSecurityPolicyDefinition(
            policyName = props["policyName"] as String,
            tableName = props["tableName"] as String,
            filterExpression = props["filterExpression"] as String,
            withCheckOption = props["withCheckOption"] as Boolean? ?: false,
            roles = (props["roles"] as? List<String>) ?: emptyList(),
        )
}
