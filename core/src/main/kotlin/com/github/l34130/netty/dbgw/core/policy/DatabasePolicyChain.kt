package com.github.l34130.netty.dbgw.core.policy

import com.github.l34130.netty.dbgw.policy.api.PolicyDecision
import com.github.l34130.netty.dbgw.policy.api.database.DatabaseAuthenticationEvent
import com.github.l34130.netty.dbgw.policy.api.database.DatabaseContext
import com.github.l34130.netty.dbgw.policy.api.database.DatabasePolicyInterceptor
import com.github.l34130.netty.dbgw.policy.api.database.query.DatabaseQueryContext

class DatabasePolicyChain<T : DatabasePolicyInterceptor>(
    val policies: List<T>,
) : DatabasePolicyInterceptor {
    override fun onAuthentication(
        ctx: DatabaseContext,
        evt: DatabaseAuthenticationEvent,
    ): PolicyDecision {
        for (policy in policies) {
            val decision = policy.onAuthentication(ctx, evt)
            if (decision is PolicyDecision.NotApplicable) continue
            return decision
        }
        return PolicyDecision.Deny(
            reason = "No policy allowed the authentication (implicit deny)",
        )
    }

    override fun onQuery(ctx: DatabaseQueryContext): PolicyDecision {
        for (policy in policies) {
            val decision = policy.onQuery(ctx)
            if (decision is PolicyDecision.NotApplicable) continue
            return decision
        }
        return PolicyDecision.Deny(
            reason = "No policy allowed the query (implicit deny)",
        )
    }
}
