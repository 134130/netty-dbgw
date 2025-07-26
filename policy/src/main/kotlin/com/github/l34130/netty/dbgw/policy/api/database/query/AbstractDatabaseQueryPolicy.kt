package com.github.l34130.netty.dbgw.policy.api.database.query

import com.github.l34130.netty.dbgw.policy.api.Resource
import com.github.l34130.netty.dbgw.policy.api.ResourceInfo

abstract class AbstractDatabaseQueryPolicy : DatabaseQueryPolicy {
    override fun getMetadata(): ResourceInfo {
        val annotation =
            this.javaClass.getAnnotation(Resource::class.java)
                ?: error("Policy metadata not found for ${this.javaClass.name}. Ensure the class is annotated with @Policy.")

        return ResourceInfo.from(annotation)
    }
}
