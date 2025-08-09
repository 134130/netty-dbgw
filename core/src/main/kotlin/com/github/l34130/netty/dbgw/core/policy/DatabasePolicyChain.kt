package com.github.l34130.netty.dbgw.core.policy

import com.github.l34130.netty.dbgw.policy.api.PolicyDecision
import com.github.l34130.netty.dbgw.policy.api.PolicyDefinition
import com.github.l34130.netty.dbgw.policy.api.database.DatabaseAuthenticationPolicyContext
import com.github.l34130.netty.dbgw.policy.api.database.DatabaseInterceptor
import com.github.l34130.netty.dbgw.policy.api.database.DatabasePolicy
import com.github.l34130.netty.dbgw.policy.api.database.DatabaseQueryPolicyContext
import com.github.l34130.netty.dbgw.policy.api.database.DatabaseResultRowPolicyContext
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.concurrent.CopyOnWriteArrayList

class DatabasePolicyChain(
    initialPolicies: List<DatabasePolicy> = emptyList(),
) : DatabaseInterceptor,
    PolicyChangeListener {
    private val policies: CopyOnWriteArrayList<DatabasePolicy> = CopyOnWriteArrayList(initialPolicies)

    override fun onAuthentication(ctx: DatabaseAuthenticationPolicyContext) {
        for (policy in policies) {
            policy.onAuthentication(ctx)
            if (ctx.decision is PolicyDecision.NotApplicable) continue
            return
        }
        ctx.decision =
            PolicyDecision.Deny(
                reason = "No policy allowed the authentication (implicit deny)",
            )
    }

    override fun onQuery(ctx: DatabaseQueryPolicyContext) {
        for (policy in policies) {
            policy.onQuery(ctx)
            if (ctx.decision is PolicyDecision.NotApplicable) continue
            return
        }
        ctx.decision =
            PolicyDecision.Deny(
                reason = "No policy allowed the query (implicit deny)",
            )
    }

    override fun onResultRow(ctx: DatabaseResultRowPolicyContext) {
        for (policy in policies) {
            policy.onResultRow(ctx)
            if (ctx.decision is PolicyDecision.NotApplicable) continue
            return
        }
        ctx.decision =
            PolicyDecision.Deny(
                reason = "No policy allowed the result row (implicit deny)",
            )
    }

    override fun onPolicyAdded(policy: PolicyDefinition) {
        val interceptor = policy.createPolicy()
        val added = policies.addIfAbsent(policy.createPolicy())
        if (added) {
            logger.info { "Policy added: $interceptor" }
        }
    }

    override fun onPolicyRemoved(policy: PolicyDefinition) {
        val removed = policies.removeIf { it.definition() == policy }
        if (removed) {
            logger.info { "Policy removed: $policy" }
        }
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}
