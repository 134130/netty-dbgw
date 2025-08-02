package com.github.l34130.netty.dbgw.policy.builtin.database.query

import com.github.l34130.netty.dbgw.policy.api.config.AbstractResourceFactory
import java.time.Clock

class DatabaseTimeRangeAccessQueryPolicyFactory :
    AbstractResourceFactory<DatabaseTimeRangeAccessPolicy>(DatabaseTimeRangeAccessPolicy::class) {
    override fun create(props: Map<String, Any>): DatabaseTimeRangeAccessPolicy =
        DatabaseTimeRangeAccessPolicy.from(
            range = props["range"] as String,
            action = DatabaseTimeRangeAccessPolicy.Action.valueOf(props["action"] as String),
            clock = props["clock"] as? Clock ?: Clock.systemDefaultZone(),
        )
}
