package com.github.l34130.netty.dbgw.core.policy

import com.github.l34130.netty.dbgw.policy.api.database.query.DatabaseQueryContext
import com.github.l34130.netty.dbgw.policy.api.database.query.DatabaseQueryPolicy
import com.github.l34130.netty.dbgw.policy.api.database.query.DatabaseQueryPolicyFactory
import com.github.l34130.netty.dbgw.policy.api.database.query.DatabaseQueryPolicyResult
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.ServiceLoader

class PolicyEngine {
    private val queryPolicies = mutableMapOf<String, DatabaseQueryPolicy>()

    fun init() {
        loadFactories()
    }

    fun evaluateQueryPolicy(ctx: DatabaseQueryContext): DatabaseQueryPolicyResult {
        queryPolicies.values.forEach { policy ->
            val result = policy.evaluate(ctx)
            if (!result.isAllowed) {
                return result
            }
        }
        return DatabaseQueryPolicyResult.Allowed()
    }

    private fun loadFactories() {
        ServiceLoader.load(DatabaseQueryPolicyFactory::class.java).forEach { queryPolicyFactory ->
            val queryPolicy =
                when {
                    queryPolicyFactory.isApplicable(
                        group = "builtin",
                        version = "v1",
                        kind = "DatabaseTimeRangeAccessQueryPolicy",
                    ) -> {
                        queryPolicyFactory.create(
                            mapOf(
                                "range" to "[15:00, 17:00)",
                                "allowInRange" to false,
                            ),
                        )
                    }
                    queryPolicyFactory.isApplicable(
                        group = "builtin",
                        version = "v1",
                        kind = "DatabaseQueryStatementType",
                    ) -> {
                        queryPolicyFactory.create(
                            mapOf(
                                "statements" to listOf("DELETE"),
                                "action" to "DENY",
                            ),
                        )
                    }
                    else -> null
                }

            if (queryPolicy != null) {
                queryPolicies[queryPolicyFactory::class.java.name] = queryPolicy
                logger.info { "Loaded query policy: $queryPolicy" }
            } else {
                logger.trace { "Query policy factory ${queryPolicyFactory::class.java.name} is not applicable" }
            }
        }
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}
