package com.github.l34130.netty.dbgw.policy.builtin.database

import com.github.l34130.netty.dbgw.policy.api.PolicyDefinition
import com.github.l34130.netty.dbgw.policy.api.database.DatabasePolicy

class DatabaseImpersonationPolicy(
    private val definition: DatabaseImpersonationPolicyDefinition,
) : DatabasePolicy {
    override fun definition(): PolicyDefinition = definition

//    override fun onAuthentication(
//        ctx: DatabaseContext,
//        evt: DatabaseAuthenticationEvent,
//    ): PolicyDecision {
//    }
}
