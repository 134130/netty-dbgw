package com.github.l34130.netty.dbgw.policy.api.query

sealed class QueryPolicyResult(
    open val isAllowed: Boolean,
    open val reason: String? = null,
) {
    data class Allowed(
        override val reason: String? = null,
    ) : QueryPolicyResult(isAllowed = true, reason = reason)

    data class Denied(
        override val reason: String? = null,
    ) : QueryPolicyResult(isAllowed = false, reason = reason)
}
