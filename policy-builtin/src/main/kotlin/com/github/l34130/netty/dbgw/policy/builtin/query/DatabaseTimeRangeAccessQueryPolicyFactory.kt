package com.github.l34130.netty.dbgw.policy.builtin.query

import com.github.l34130.netty.dbgw.policy.api.query.DatabaseQueryPolicy
import com.github.l34130.netty.dbgw.policy.api.query.DatabaseQueryPolicyFactory
import java.time.Clock

class DatabaseTimeRangeAccessQueryPolicyFactory : DatabaseQueryPolicyFactory {
    override fun isApplicable(
        group: String,
        version: String,
        kind: String,
    ): Boolean = DatabaseTimeRangeAccessQueryPolicy.METADATA.isApplicable(group, version, kind)

    override fun create(props: Map<String, Any>): DatabaseQueryPolicy =
        DatabaseTimeRangeAccessQueryPolicy.from(
            range = props["range"] as String,
            allowInRange = props["allowInRange"] as Boolean? ?: true,
            clock = props["clock"] as? Clock ?: Clock.systemDefaultZone(),
        )
}
