package com.github.l34130.netty.dbgw.policy.api

import com.github.l34130.netty.dbgw.policy.api.database.DatabaseAuthenticationEvent
import com.github.l34130.netty.dbgw.policy.api.database.DatabaseContext
import com.github.l34130.netty.dbgw.policy.api.database.DatabasePolicyInterceptor
import com.github.l34130.netty.dbgw.policy.api.database.query.DatabaseQueryContext

interface PolicyDefinition {
    fun createInterceptor(): DatabasePolicyInterceptor

    companion object {
        val ALLOW_ALL: PolicyDefinition =
            object : PolicyDefinition {
                override fun createInterceptor(): DatabasePolicyInterceptor =
                    object : DatabasePolicyInterceptor {
                        override fun onAuthentication(
                            ctx: DatabaseContext,
                            evt: DatabaseAuthenticationEvent,
                        ): PolicyDecision = PolicyDecision.Allow(reason = "All authentications are allowed")

                        override fun onQuery(ctx: DatabaseQueryContext): PolicyDecision =
                            PolicyDecision.Allow(reason = "All queries are allowed")
                    }
            }
    }
}
