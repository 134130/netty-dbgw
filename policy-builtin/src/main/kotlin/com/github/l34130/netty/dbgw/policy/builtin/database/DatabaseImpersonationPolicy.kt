package com.github.l34130.netty.dbgw.policy.builtin.database

import com.github.l34130.netty.dbgw.policy.api.PolicyDefinition
import com.github.l34130.netty.dbgw.policy.api.database.DatabaseAuthenticationPolicyContext
import com.github.l34130.netty.dbgw.policy.api.database.DatabasePolicy

class DatabaseImpersonationPolicy(
    private val definition: DatabaseImpersonationPolicyDefinition,
) : DatabasePolicy {
    override fun definition(): PolicyDefinition = definition

    override fun onAuthentication(ctx: DatabaseAuthenticationPolicyContext) {
        val user = definition.action?.user
        user?.takeIf { it.isNotBlank() }?.let { ctx.username = it }
        val password = definition.action?.password
        password?.takeIf { it.isNotBlank() }?.let { ctx.password = it }
    }
}
