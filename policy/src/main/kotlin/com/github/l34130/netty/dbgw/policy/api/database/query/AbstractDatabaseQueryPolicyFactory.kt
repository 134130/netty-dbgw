package com.github.l34130.netty.dbgw.policy.api.database.query

import com.github.l34130.netty.dbgw.policy.api.Resource
import com.github.l34130.netty.dbgw.policy.api.ResourceInfo
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

abstract class AbstractDatabaseQueryPolicyFactory<T : DatabaseQueryPolicy>(
    private val policyClass: KClass<T>,
) : DatabaseQueryPolicyFactory<T> {
    override fun isApplicable(
        group: String,
        version: String,
        kind: String,
    ): Boolean {
        val resourceAnnotation =
            policyClass.findAnnotation<Resource>()
                ?: error("Policy metadata not found for ${policyClass.qualifiedName}. Ensure the class is annotated with @Policy.")
        return ResourceInfo.from(resourceAnnotation).isApplicable(group, version, kind)
    }
}
