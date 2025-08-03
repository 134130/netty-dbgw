package com.github.l34130.netty.dbgw.policy.builtin.database

import com.github.l34130.netty.dbgw.policy.api.config.AbstractResourceFactory

class DatabaseResultSetMaskingPolicyDefinitionFactory :
    AbstractResourceFactory<DatabaseResultSetMaskingPolicyDefinition>(DatabaseResultSetMaskingPolicyDefinition::class) {
    override fun create(props: Map<String, Any>): DatabaseResultSetMaskingPolicyDefinition =
        DatabaseResultSetMaskingPolicyDefinition(
            maskingRegex = props["maskingRegex"] as String,
        )
}
