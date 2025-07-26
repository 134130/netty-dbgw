package com.github.l34130.netty.dbgw.policy.api.database.query

import com.github.l34130.netty.dbgw.policy.api.ResourceInfo

interface DatabaseQueryPolicy {
    fun evaluate(ctx: DatabaseQueryContext): DatabaseQueryPolicyResult

    fun getMetadata(): ResourceInfo
}
