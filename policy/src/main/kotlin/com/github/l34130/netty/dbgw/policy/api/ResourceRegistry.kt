package com.github.l34130.netty.dbgw.policy.api

import com.github.l34130.netty.dbgw.policy.api.ResourceInfo.Names
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

interface ResourceRegistry {
    fun registerResource(resource: ResourceInfo)

    fun registerResourceAnnotated(clazz: KClass<*>)

    fun unregisterResource(gvr: GroupVersionResource)

    fun getResourceByGvr(gvr: GroupVersionResource): ResourceInfo?

    fun getResourceByGvk(gvk: GroupVersionKind): ResourceInfo?

    companion object {
        val DEFAULT: ResourceRegistry =
            object : ResourceRegistry {
                private val logger = KotlinLogging.logger { }

                private val lock = ReentrantReadWriteLock()

                private val resourcesByGvr = mutableMapOf<GroupVersionResource, ResourceInfo>()
                private val gvrByGvk = mutableMapOf<GroupVersionKind, GroupVersionResource>()

                override fun registerResource(resource: ResourceInfo) {
                    lock.write {
                        val gvr = resource.groupVersionResource()
                        val gvk = resource.groupVersionKind()

                        if (resourcesByGvr.containsKey(gvr) || gvrByGvk.containsKey(gvk)) {
                            throw IllegalArgumentException("Resource with GVR $gvr or GVK $gvk already registered.")
                        }

                        resourcesByGvr[gvr] = resource
                        gvrByGvk[gvk] = gvr
                    }

                    logger.debug { "Registered resource: $resource" }
                }

                override fun registerResourceAnnotated(clazz: KClass<*>) {
                    val resourceAnnotation =
                        clazz.findAnnotation<Resource>()
                            ?: throw IllegalArgumentException("Class ${clazz.simpleName} is not annotated with @Resource")

                    val resourceInfo =
                        ResourceInfo(
                            group = resourceAnnotation.group,
                            version = resourceAnnotation.version,
                            names =
                                if (resourceAnnotation.singular.isEmpty()) {
                                    Names(
                                        kind = resourceAnnotation.kind,
                                        plural = resourceAnnotation.plural,
                                    )
                                } else {
                                    Names(
                                        kind = resourceAnnotation.kind,
                                        plural = resourceAnnotation.plural,
                                        singular = resourceAnnotation.singular,
                                    )
                                },
                        )
                    registerResource(resourceInfo)
                }

                override fun unregisterResource(gvr: GroupVersionResource) {
                    lock.write {
                        val resource = resourcesByGvr.remove(gvr)
                        if (resource != null) {
                            val gvk = resource.groupVersionKind()
                            gvrByGvk.remove(gvk)
                        } else {
                            throw IllegalArgumentException("Resource not found: $resource")
                        }
                    }
                }

                override fun getResourceByGvr(gvr: GroupVersionResource): ResourceInfo? = lock.read { resourcesByGvr[gvr] }

                override fun getResourceByGvk(gvk: GroupVersionKind): ResourceInfo? =
                    lock.read {
                        gvrByGvk[gvk]?.let { gvr -> resourcesByGvr[gvr] }
                    }
            }
    }
}
