package com.github.l34130.netty.dbgw.policy.api.database.query

interface DatabaseQueryPolicyFactory<T : DatabaseQueryPolicy> {
    fun isApplicable(
        group: String,
        version: String,
        kind: String,
    ): Boolean

    fun create(props: Map<String, Any>): T
}
