package com.github.l34130.netty.dbgw.policy.api

import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

abstract class AbstractResourceFactory<T : Any>(
    private val policyClass: KClass<T>,
) : ResourceFactory<T> {
    override fun type(): KClass<T> = policyClass

    override fun isApplicable(gvk: GroupVersionKind): Boolean {
        val resourceAnnotation =
            policyClass.findAnnotation<Resource>()
                ?: error("Resource metadata not found for ${policyClass.qualifiedName}. Ensure the class is annotated with @Resource.")
        val resource =
            ResourceRegistry.DEFAULT.getResourceByGvk(gvk)
                ?: error("Resource not found for GVK: $gvk. Ensure the resource is registered in the ResourceRegistry.")
        return resource.group == resourceAnnotation.group &&
            resource.version == resourceAnnotation.version &&
            resource.names.kind == resourceAnnotation.kind
    }
}
