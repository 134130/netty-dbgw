package com.github.l34130.netty.dbgw.policy.api.database

import com.github.l34130.netty.dbgw.policy.api.PolicyDecision

interface DatabaseInterceptor {
    fun onAuthentication(ctx: DatabaseAuthenticationPolicyContext) {
        ctx.decision = PolicyDecision.NotApplicable
    }

    fun onQuery(ctx: DatabaseQueryPolicyContext) {
        ctx.decision = PolicyDecision.NotApplicable
    }

    fun onResultRow(ctx: DatabaseResultRowPolicyContext) {
        ctx.decision = PolicyDecision.NotApplicable
    }
}
