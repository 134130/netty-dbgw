package com.github.l34130.netty.dbgw.policy.api.database.query

sealed class DatabaseQueryPolicyResult(
    open val isAllowed: Boolean,
    open val reason: String? = null,
) {
    data class Allowed(
        override val reason: String? = null,
    ) : DatabaseQueryPolicyResult(isAllowed = true, reason = reason)

    data class Denied(
        override val reason: String? = null,
    ) : DatabaseQueryPolicyResult(isAllowed = false, reason = reason)
}
