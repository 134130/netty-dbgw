package com.github.l34130.netty.dbgw.core.policy

import com.github.l34130.netty.dbgw.policy.api.ManifestMapper
import com.github.l34130.netty.dbgw.policy.api.ResourceFactory
import com.github.l34130.netty.dbgw.policy.api.ResourceRegistry
import com.github.l34130.netty.dbgw.policy.api.database.query.DatabaseQueryContext
import com.github.l34130.netty.dbgw.policy.api.database.query.DatabaseQueryPolicy
import com.github.l34130.netty.dbgw.policy.api.database.query.DatabaseQueryPolicyResult
import java.io.File
import java.util.ServiceLoader
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSuperclassOf

class PolicyEngine(
    val policies: List<Any> = emptyList(),
) {
    private val queryPolicies: List<DatabaseQueryPolicy> = policies.filterIsInstance<DatabaseQueryPolicy>()

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getPolicy(policyClass: KClass<T>): T? {
        return policies.firstOrNull { policyClass.isInstance(it) } as T?
    }

    fun evaluateQueryPolicy(ctx: DatabaseQueryContext): DatabaseQueryPolicyResult {
        queryPolicies.forEach { policy ->
            val result = policy.evaluate(ctx)
            if (!result.isAllowed) {
                return result
            }
        }
        return DatabaseQueryPolicyResult.Allowed()
    }

    companion object {
        init {
            ServiceLoader.load(ResourceFactory::class.java).forEach { factory ->
                ResourceRegistry.DEFAULT.registerResourceAnnotated(factory.type())
            }
        }

        /**
         * Loads a policy engine from a manifest file or directory.
         */
        fun loadFromManifest(file: File): PolicyEngine {
            require(file.exists()) { "Policy file or directory does not exist: $file" }

            if (file.isFile) {
                require(file.extension == "yaml" || file.extension == "yml") {
                    "Expected a YAML file, but got: ${file.extension}"
                }

                return PolicyEngine(loadFromFile(file))
            } else {
                val policies = mutableListOf<Any>()
                file.walk().forEach { file ->
                    if (file.isFile && file.extension == "yaml" || file.extension == "yml") {
                        policies.addAll(loadFromFile(file))
                    }
                }
                return PolicyEngine(policies = policies)
            }
        }

        private fun loadFromFile(file: File): List<Any> {
            require(!file.isDirectory) { "Expected a file, but got a directory: $file" }
            require(file.exists()) { "Policy file does not exist: $file" }
            require(file.extension == "yaml") { "Expected a YAML file, but got: ${file.extension}" }

            val manifests =
                com.github.l34130.netty.dbgw.policy.api.ManifestMapper
                    .readValues(file)
                    .map { it.groupVersionKind() to it }

            val policies = mutableListOf<Any>()
            val factories = ServiceLoader.load(com.github.l34130.netty.dbgw.policy.api.ResourceFactory::class.java).toList()
            for ((gvk, manifest) in manifests) {
                factories.find { factory ->
                    val resourceClass = factory.type()
                    val resourceAnnotation = resourceClass.findAnnotation<com.github.l34130.netty.dbgw.policy.api.Resource>()
                    if (resourceAnnotation != null) {
                        val resourceInfo = com.github.l34130.netty.dbgw.policy.api.ResourceInfo(
                            group = resourceAnnotation.group,
                            version = resourceAnnotation.version,
                            names = com.github.l34130.netty.dbgw.policy.api.ResourceInfo.Names(
                                kind = resourceAnnotation.kind,
                                plural = resourceAnnotation.plural,
                                singular = resourceAnnotation.singular
                            )
                        )
                        resourceInfo.groupVersionKind() == gvk
                    } else {
                        false
                    }
                }?.let { factory ->
                    policies.add(factory.create(com.github.l34130.netty.dbgw.policy.api.ManifestMapper.Default.valueToTree(manifest.spec)))
                }
            }
            return policies
        }
    }
}
