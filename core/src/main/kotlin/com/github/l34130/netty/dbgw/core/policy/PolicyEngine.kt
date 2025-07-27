package com.github.l34130.netty.dbgw.core.policy

import com.github.l34130.netty.dbgw.policy.api.ManifestMapper
import com.github.l34130.netty.dbgw.policy.api.ResourceFactory
import com.github.l34130.netty.dbgw.policy.api.ResourceRegistry
import com.github.l34130.netty.dbgw.policy.api.database.query.DatabaseQueryContext
import com.github.l34130.netty.dbgw.policy.api.database.query.DatabaseQueryPolicy
import com.github.l34130.netty.dbgw.policy.api.database.query.DatabaseQueryPolicyResult
import java.io.File
import java.util.ServiceLoader
import kotlin.reflect.full.isSuperclassOf

class PolicyEngine(
    val queryPolicies: List<DatabaseQueryPolicy> = emptyList(),
) {
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
                val queryPolicies = mutableListOf<DatabaseQueryPolicy>()
                file.walk().forEach { file ->
                    if (file.isFile && file.extension == "yaml" || file.extension == "yml") {
                        queryPolicies.addAll(loadFromFile(file))
                    }
                }
                return PolicyEngine(queryPolicies = queryPolicies)
            }
        }

        private fun loadFromFile(file: File): List<DatabaseQueryPolicy> {
            require(!file.isDirectory) { "Expected a file, but got a directory: $file" }
            require(file.exists()) { "Policy file does not exist: $file" }
            require(file.extension == "yaml") { "Expected a YAML file, but got: ${file.extension}" }

            val manifests =
                ManifestMapper
                    .readValues(file)
                    .map { it.groupVersionKind() to it }

            val queryPolicies = mutableListOf<DatabaseQueryPolicy>()
            ServiceLoader.load(ResourceFactory::class.java).forEach { factory ->
                for ((gvk, manifest) in manifests) {
                    if (DatabaseQueryPolicy::class.isSuperclassOf(factory.type()) && factory.isApplicable(gvk)) {
                        queryPolicies.add(factory.create(manifest.spec) as DatabaseQueryPolicy)
                    }
                }
            }
            return queryPolicies
        }
    }
}
