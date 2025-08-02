package com.github.l34130.netty.dbgw.policy.api

sealed interface PolicyDecision {
    class Allow(
        val reason: String? = null,
    ) : PolicyDecision

    class Deny(
        val reason: String? = null,
    ) : PolicyDecision

    object NotApplicable : PolicyDecision
}
