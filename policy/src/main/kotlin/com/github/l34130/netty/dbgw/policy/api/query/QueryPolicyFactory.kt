package com.github.l34130.netty.dbgw.policy.api.query

interface QueryPolicyFactory {
    fun isApplicable(
        group: String,
        version: String,
        kind: String,
    ): Boolean

    fun create(props: Map<String, Any>): QueryPolicy
}
