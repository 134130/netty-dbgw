package com.github.l34130.netty.dbgw.policy.builtin.database.query

import com.github.l34130.netty.dbgw.policy.api.config.AbstractResourceFactory

class DatabaseResultSetMaskingPolicyDefinitionFactory :
    AbstractResourceFactory<DatabaseResultSetMaskingPolicy>(DatabaseResultSetMaskingPolicy::class) {
    override fun create(props: Map<String, Any>): DatabaseResultSetMaskingPolicy =
        DatabaseResultSetMaskingPolicy(
            maskingRegex = Regex(props["maskingRegex"] as String),
        )
}
