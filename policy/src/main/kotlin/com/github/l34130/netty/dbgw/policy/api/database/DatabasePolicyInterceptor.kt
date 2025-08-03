package com.github.l34130.netty.dbgw.policy.api.database

import com.github.l34130.netty.dbgw.policy.api.PolicyDecision
import com.github.l34130.netty.dbgw.policy.api.database.query.DatabaseQueryContext
import com.github.l34130.netty.dbgw.policy.api.database.query.DatabaseResultRowContext

interface DatabasePolicyInterceptor {
    fun onAuthentication(
        ctx: DatabaseContext,
        evt: DatabaseAuthenticationEvent,
    ): PolicyDecision = PolicyDecision.NotApplicable

    fun onQuery(ctx: DatabaseQueryContext): PolicyDecision = PolicyDecision.NotApplicable

    fun onResultRow(ctx: DatabaseResultRowContext): Unit = Unit
}
