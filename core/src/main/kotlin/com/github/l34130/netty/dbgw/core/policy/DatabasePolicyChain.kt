package com.github.l34130.netty.dbgw.core.policy

import com.github.l34130.netty.dbgw.policy.api.PolicyDecision
import com.github.l34130.netty.dbgw.policy.api.PolicyDefinition
import com.github.l34130.netty.dbgw.policy.api.database.DatabaseAuthenticationEvent
import com.github.l34130.netty.dbgw.policy.api.database.DatabaseContext
import com.github.l34130.netty.dbgw.policy.api.database.DatabasePolicy
import com.github.l34130.netty.dbgw.policy.api.database.DatabasePolicyInterceptor
import com.github.l34130.netty.dbgw.policy.api.database.DatabaseQueryEvent
import com.github.l34130.netty.dbgw.policy.api.database.query.DatabaseResultRowContext
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.concurrent.CopyOnWriteArrayList

class DatabasePolicyChain(
    initialPolicies: List<DatabasePolicy> = emptyList(),
) : DatabasePolicyInterceptor,
    PolicyChangeListener {
    private val policies: CopyOnWriteArrayList<DatabasePolicy> = CopyOnWriteArrayList(initialPolicies)

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

    override fun onQuery(
        ctx: DatabaseContext,
        evt: DatabaseQueryEvent,
    ): PolicyDecision {
        for (policy in policies) {
            val decision = policy.onQuery(ctx, evt)
            if (decision is PolicyDecision.NotApplicable) continue
            return decision
        }
        return PolicyDecision.Deny(
            reason = "No policy allowed the query (implicit deny)",
        )
    }

    override fun onResultRow(ctx: DatabaseResultRowContext): PolicyDecision {
        for (policy in policies) {
            val decision = policy.onResultRow(ctx)
            if (decision is PolicyDecision.NotApplicable) continue
            return decision
        }
        return PolicyDecision.Deny(
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
