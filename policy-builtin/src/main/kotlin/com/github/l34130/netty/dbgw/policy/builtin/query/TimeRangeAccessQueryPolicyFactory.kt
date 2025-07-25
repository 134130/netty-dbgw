package com.github.l34130.netty.dbgw.policy.builtin.query

import com.github.l34130.netty.dbgw.policy.api.query.QueryPolicy
import com.github.l34130.netty.dbgw.policy.api.query.QueryPolicyFactory
import java.time.Clock

class TimeRangeAccessQueryPolicyFactory : QueryPolicyFactory {
    override fun isApplicable(
        group: String,
        version: String,
        kind: String,
    ): Boolean = TimeRangeAccessQueryPolicy.METADATA.isApplicable(group, version, kind)

    override fun create(props: Map<String, Any>): QueryPolicy =
        TimeRangeAccessQueryPolicy.from(
            range = props["range"] as String,
            allowInRange = props["allowInRange"] as Boolean? ?: true,
            clock = props["clock"] as? Clock ?: Clock.systemDefaultZone(),
        )
}
