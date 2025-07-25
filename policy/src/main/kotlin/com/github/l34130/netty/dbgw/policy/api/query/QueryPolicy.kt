package com.github.l34130.netty.dbgw.policy.api.query

import com.github.l34130.netty.dbgw.policy.api.PolicyMetadata

interface QueryPolicy {
    fun evaluate(
        ctx: QueryPolicyContext,
        query: String,
    ): QueryPolicyResult

    fun getMetadata(): PolicyMetadata
}
