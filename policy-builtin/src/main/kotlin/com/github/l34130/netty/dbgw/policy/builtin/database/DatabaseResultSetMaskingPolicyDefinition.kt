package com.github.l34130.netty.dbgw.policy.builtin.database

import com.github.l34130.netty.dbgw.policy.api.PolicyDefinition
import com.github.l34130.netty.dbgw.policy.api.config.Resource
import com.github.l34130.netty.dbgw.policy.api.database.DatabasePolicyInterceptor

@Resource(
    group = "builtin",
    version = "v1",
    kind = "DatabaseResultSetMaskingPolicy",
    plural = "databaseresultsetmaskingpolicies",
    singular = "databaseresultsetmaskingpolicy",
)
data class DatabaseResultSetMaskingPolicyDefinition(
    val maskingRegex: String,
) : PolicyDefinition {
    override fun createInterceptor(): DatabasePolicyInterceptor =
        DatabaseResultSetMaskingPolicy(
            maskingRegex = Regex(maskingRegex),
        )
}
