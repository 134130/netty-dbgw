package com.github.l34130.netty.dbgw.policy.api.database.query

interface DatabaseQueryPolicy {
    fun evaluate(ctx: DatabaseQueryContext): DatabaseQueryPolicyResult
}
