package com.github.l34130.netty.dbgw.policy.builtin.database.query

import com.github.l34130.netty.dbgw.policy.api.config.AbstractResourceFactory
import java.time.Clock

class DatabaseTimeRangeAccessQueryPolicyFactory :
    AbstractResourceFactory<DatabaseTimeRangeAccessQueryPolicy>(DatabaseTimeRangeAccessQueryPolicy::class) {
    override fun create(props: Map<String, Any>): DatabaseTimeRangeAccessQueryPolicy =
        DatabaseTimeRangeAccessQueryPolicy.from(
            range = props["range"] as String,
            allowInRange = props["allowInRange"] as Boolean? ?: true,
            clock = props["clock"] as? Clock ?: Clock.systemDefaultZone(),
        )
}
