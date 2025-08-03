package com.github.l34130.netty.dbgw.core.policy

import com.github.l34130.netty.dbgw.policy.api.PolicyDecision
import com.github.l34130.netty.dbgw.policy.api.PolicyDefinition
import com.github.l34130.netty.dbgw.policy.api.database.DatabaseAuthenticationEvent
import com.github.l34130.netty.dbgw.policy.api.database.DatabaseContext
import com.github.l34130.netty.dbgw.policy.api.database.DatabasePolicyInterceptor
import com.github.l34130.netty.dbgw.policy.api.database.query.DatabaseQueryContext
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.concurrent.CopyOnWriteArrayList

class DatabasePolicyChain(
    initialPolicies: List<Pair<PolicyDefinition, DatabasePolicyInterceptor>> = emptyList(),
) : DatabasePolicyInterceptor,
    PolicyChangeListener {
    private val policies: CopyOnWriteArrayList<Pair<PolicyDefinition, DatabasePolicyInterceptor>> = CopyOnWriteArrayList(initialPolicies)

    override fun onAuthentication(
        ctx: DatabaseContext,
        evt: DatabaseAuthenticationEvent,
    ): PolicyDecision {
        for (policy in policies.map { it.second }) {
            val decision = policy.onAuthentication(ctx, evt)
            if (decision is PolicyDecision.NotApplicable) continue
            return decision
        }
        return PolicyDecision.Deny(
            reason = "No policy allowed the authentication (implicit deny)",
        )
    }

    override fun onQuery(ctx: DatabaseQueryContext): PolicyDecision {
        for (policy in policies.map { it.second }) {
            val decision = policy.onQuery(ctx)
            if (decision is PolicyDecision.NotApplicable) continue
            return decision
        }
        return PolicyDecision.Deny(
            reason = "No policy allowed the query (implicit deny)",
        )
    }

    override fun onPolicyAdded(policy: PolicyDefinition) {
        val interceptor = policy.createInterceptor()
        val added = policies.addIfAbsent(policy to interceptor)
        if (added) {
            logger.info { "Policy added: $interceptor" }
        }
    }

    override fun onPolicyRemoved(policy: PolicyDefinition) {
        val removed = policies.removeIf { it.first == policy }
        if (removed) {
            logger.info { "Policy removed: $policy" }
        }
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}
