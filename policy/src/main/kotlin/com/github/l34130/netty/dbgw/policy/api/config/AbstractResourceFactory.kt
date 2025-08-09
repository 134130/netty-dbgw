package com.github.l34130.netty.dbgw.policy.api.config

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

abstract class AbstractResourceFactory<T : Any>(
    private val policyClass: KClass<T>,
) : ResourceFactory<T> {
    private val logger = KotlinLogging.logger { }

    override fun type(): KClass<T> = policyClass

    override fun isApplicable(gvk: GroupVersionKind): Boolean {
        val resourceAnnotation =
            policyClass.findAnnotation<Resource>()
                ?: error("Resource metadata not found for ${policyClass.qualifiedName}. Ensure the class is annotated with @Resource.")
        val resource = ResourceRegistry.DEFAULT.getResourceByGvk(gvk)
        if (resource == null) {
            logger.warn { "Resource $gvk not found in registry for ${policyClass.qualifiedName}. Ensure the resource is registered." }
            return false
        }

        return resource.group == resourceAnnotation.group &&
            resource.version == resourceAnnotation.version &&
            resource.names.kind == resourceAnnotation.kind
    }

    override fun create(props: Map<String, Any>): T = objectMapper.convertValue(props, policyClass.java)

    companion object {
        protected val objectMapper = jacksonObjectMapper()
    }
}
