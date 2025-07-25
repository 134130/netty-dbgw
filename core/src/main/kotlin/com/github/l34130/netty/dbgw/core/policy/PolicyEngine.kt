package com.github.l34130.netty.dbgw.core.policy

import com.github.l34130.netty.dbgw.policy.api.query.QueryPolicy
import com.github.l34130.netty.dbgw.policy.api.query.QueryPolicyContext
import com.github.l34130.netty.dbgw.policy.api.query.QueryPolicyResult
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.ServiceLoader

class PolicyEngine {
    private val queryPolicies = mutableMapOf<String, QueryPolicy>()

    fun init() {
        loadFactories()
    }

    fun evaluateQueryPolicy(
        ctx: QueryPolicyContext,
        query: String,
    ): QueryPolicyResult {
        queryPolicies.values.forEach { policy ->
            val result = policy.evaluate(ctx, query)
            if (!result.isAllowed) {
                return result
            }
        }
        return QueryPolicyResult.Allowed()
    }

    private fun loadFactories() {
        ServiceLoader.load(QueryPolicy::class.java).forEach { queryPolicy ->
            if (queryPolicies.containsKey(queryPolicy.getMetadata().key())) {
                logger.warn { "Duplicate query policy found: ${queryPolicy.getMetadata().key()}" }
            } else {
                queryPolicies[queryPolicy.getMetadata().key()] = queryPolicy
                logger.debug { "Loaded query policy: ${queryPolicy.getMetadata().key()}" }
            }
        }
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}
