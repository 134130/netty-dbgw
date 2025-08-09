package com.github.l34130.netty.dbgw.policy.builtin.database

import com.github.l34130.netty.dbgw.policy.api.PolicyDefinition
import com.github.l34130.netty.dbgw.policy.api.config.AbstractResourceFactory
import com.github.l34130.netty.dbgw.policy.api.config.Resource
import com.github.l34130.netty.dbgw.policy.api.database.DatabasePolicy

@Resource(
    group = "builtin",
    version = "v1",
    kind = "DatabaseImpersonationPolicy",
    plural = "databaseimpersonationpolicies",
    singular = "databaseimpersonationpolicy",
)
class DatabaseImpersonationPolicyDefinition(
    val action: Action?,
) : PolicyDefinition {
    override fun createPolicy(): DatabasePolicy = DatabaseImpersonationPolicy(this)

    data class Action(
        val user: String?,
        val password: String?,
    )

    class Factory : AbstractResourceFactory<DatabaseImpersonationPolicyDefinition>(DatabaseImpersonationPolicyDefinition::class)
}
