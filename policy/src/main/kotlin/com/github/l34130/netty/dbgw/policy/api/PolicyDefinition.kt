package com.github.l34130.netty.dbgw.policy.api

import com.github.l34130.netty.dbgw.policy.api.database.DatabaseAuthenticationPolicyContext
import com.github.l34130.netty.dbgw.policy.api.database.DatabasePolicy
import com.github.l34130.netty.dbgw.policy.api.database.DatabaseQueryPolicyContext
import com.github.l34130.netty.dbgw.policy.api.database.DatabaseResultRowPolicyContext

interface PolicyDefinition {
    fun createPolicy(): DatabasePolicy

    companion object {
        val ALLOW_ALL: PolicyDefinition =
            object : PolicyDefinition {
                override fun createPolicy(): DatabasePolicy =
                    object : DatabasePolicy {
                        override fun definition(): PolicyDefinition = ALLOW_ALL

                        override fun onAuthentication(ctx: DatabaseAuthenticationPolicyContext) {
                            ctx.decision =
                                PolicyDecision.Allow(
                                    reason = "All authentications are allowed",
                                )
                        }

                        override fun onQuery(ctx: DatabaseQueryPolicyContext) {
                            ctx.decision =
                                PolicyDecision.Allow(
                                    reason = "All queries are allowed",
                                )
                        }

                        override fun onResultRow(ctx: DatabaseResultRowPolicyContext) {
                            ctx.decision =
                                PolicyDecision.Allow(
                                    reason = "All result rows are allowed",
                                )
                        }
                    }
            }
    }
}
