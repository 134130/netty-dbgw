package com.github.l34130.netty.dbgw.policy.api.query

import com.github.l34130.netty.dbgw.policy.api.PolicyMetadata

interface DatabaseQueryPolicy {
    fun evaluate(ctx: DatabaseQueryContext): DatabaseQueryPolicyResult

    fun getMetadata(): PolicyMetadata
}
