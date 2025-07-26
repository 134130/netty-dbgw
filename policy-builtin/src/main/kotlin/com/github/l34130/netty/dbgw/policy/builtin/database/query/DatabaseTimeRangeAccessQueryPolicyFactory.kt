package com.github.l34130.netty.dbgw.policy.builtin.database.query

import com.github.l34130.netty.dbgw.policy.api.database.query.AbstractDatabaseQueryPolicyFactory
import java.time.Clock

class DatabaseTimeRangeAccessQueryPolicyFactory :
    AbstractDatabaseQueryPolicyFactory<DatabaseTimeRangeAccessQueryPolicy>(DatabaseTimeRangeAccessQueryPolicy::class) {
    override fun create(props: Map<String, Any>): DatabaseTimeRangeAccessQueryPolicy =
        DatabaseTimeRangeAccessQueryPolicy.from(
            range = props["range"] as String,
            allowInRange = props["allowInRange"] as Boolean? ?: true,
            clock = props["clock"] as? Clock ?: Clock.systemDefaultZone(),
        )
}
