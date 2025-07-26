package com.github.l34130.netty.dbgw.policy.api.database.query

import com.github.l34130.netty.dbgw.policy.api.Policy
import com.github.l34130.netty.dbgw.policy.api.PolicyDefinition

abstract class AbstractDatabaseQueryPolicy : DatabaseQueryPolicy {
    override fun getMetadata(): PolicyDefinition {
        val annotation =
            this.javaClass.getAnnotation(Policy::class.java)
                ?: error("Policy metadata not found for ${this.javaClass.name}. Ensure the class is annotated with @Policy.")

        return PolicyDefinition.from(annotation)
    }
}
