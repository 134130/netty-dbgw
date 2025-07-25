package com.github.l34130.netty.dbgw.core.security

@Deprecated("Use com.github.l34130.netty.dbgw.policy.api.query.QueryPolicy instead")
fun interface QueryPolicy {
    fun evaluate(query: String): QueryPolicyResult
}

@Deprecated("Use com.github.l34130.netty.dbgw.policy.api.query.QueryPolicyResult instead")
data class QueryPolicyResult(
    val isAllowed: Boolean,
    val modifiedQuery: String? = null,
    val reason: String? = null,
)

@Deprecated("Use com.github.l34130.netty.dbgw.core.policy.QueryPolicyEngine instead")
class QueryPolicyEngine(
    private val policies: List<QueryPolicy>,
) {
    fun evaluate(query: String): QueryPolicyResult {
        var currentQuery = query
        for (policy in policies) {
            val result = policy.evaluate(currentQuery)
            if (!result.isAllowed) {
                return QueryPolicyResult(isAllowed = false, reason = result.reason)
            }
            currentQuery = result.modifiedQuery ?: currentQuery
        }
        return QueryPolicyResult(isAllowed = true, modifiedQuery = currentQuery)
    }
}
