package com.github.l34130.netty.dbgw.policy.api.database.query

import com.github.l34130.netty.dbgw.policy.api.PolicyDefinition

interface DatabaseQueryPolicy {
    fun evaluate(ctx: DatabaseQueryContext): DatabaseQueryPolicyResult

    fun getMetadata(): PolicyDefinition
}
