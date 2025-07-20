package com.github.l34130.netty.dbgw.core.security

fun interface QueryPolicy {
    fun evaluate(query: String): QueryPolicyResult
}

data class QueryPolicyResult(
    val isAllowed: Boolean,
    val modifiedQuery: String? = null,
    val reason: String? = null,
)

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
