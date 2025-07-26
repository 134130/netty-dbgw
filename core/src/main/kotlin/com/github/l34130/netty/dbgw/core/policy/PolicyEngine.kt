package com.github.l34130.netty.dbgw.core.policy

import com.github.l34130.netty.dbgw.policy.api.query.QueryPolicy
import com.github.l34130.netty.dbgw.policy.api.query.QueryPolicyContext
import com.github.l34130.netty.dbgw.policy.api.query.QueryPolicyFactory
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
        ServiceLoader.load(QueryPolicyFactory::class.java).forEach { queryPolicyFactory ->
            val queryPolicy =
                if (queryPolicyFactory.isApplicable(
                        group = "builtin",
                        version = "v1",
                        kind = "TimeRangeAccessQueryPolicy",
                    )
                ) {
                    queryPolicyFactory.create(
                        mapOf(
                            "range" to "[15:00, 17:00)",
                            "allowInRange" to false,
                        ),
                    )
                } else {
                    null
                }

            if (queryPolicy != null) {
                queryPolicies[queryPolicyFactory::class.java.name] = queryPolicy
                logger.info { "Loaded query policy: ${queryPolicyFactory::class.java.name}" }
            } else {
                logger.trace { "Query policy factory ${queryPolicyFactory::class.java.name} is not applicable" }
            }
        }
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}
