package com.github.l34130.netty.dbgw.policy.builtin.database

import com.github.l34130.netty.dbgw.policy.api.config.AbstractResourceFactory
import java.time.Clock

class DatabaseTimeRangeAccessPolicyDefinitionFactory :
    AbstractResourceFactory<DatabaseTimeRangeAccessPolicyDefinition>(DatabaseTimeRangeAccessPolicyDefinition::class) {
    override fun create(props: Map<String, Any>): DatabaseTimeRangeAccessPolicyDefinition =
        DatabaseTimeRangeAccessPolicyDefinition.from(
            range = props["range"] as String,
            action = DatabaseTimeRangeAccessPolicyDefinition.Action.valueOf(props["action"] as String),
            clock = props["clock"] as Clock? ?: Clock.systemDefaultZone(),
        )
}
