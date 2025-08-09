package com.github.l34130.netty.dbgw.policy.api

import com.github.l34130.netty.dbgw.policy.api.database.DatabaseAuthenticationEvent
import com.github.l34130.netty.dbgw.policy.api.database.DatabaseContext
import com.github.l34130.netty.dbgw.policy.api.database.DatabasePolicy
import com.github.l34130.netty.dbgw.policy.api.database.DatabaseQueryEvent
import com.github.l34130.netty.dbgw.policy.api.database.query.DatabaseResultRowContext

interface PolicyDefinition {
    fun createPolicy(): DatabasePolicy

    companion object {
        val ALLOW_ALL: PolicyDefinition =
            object : PolicyDefinition {
                override fun createPolicy(): DatabasePolicy =
                    object : DatabasePolicy {
                        override fun definition(): PolicyDefinition = ALLOW_ALL

                        override fun onAuthentication(
                            ctx: DatabaseContext,
                            evt: DatabaseAuthenticationEvent,
                        ): PolicyDecision = PolicyDecision.Allow(reason = "All authentications are allowed")

                        override fun onQuery(
                            ctx: DatabaseContext,
                            evt: DatabaseQueryEvent,
                        ): PolicyDecision = PolicyDecision.Allow(reason = "All queries are allowed")

                        override fun onResultRow(ctx: DatabaseResultRowContext): PolicyDecision =
                            PolicyDecision.Allow(
                                reason = "All result rows are allowed",
                            )
                    }
            }
    }
}
