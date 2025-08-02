package com.github.l34130.netty.dbgw.core.policy

import com.github.l34130.netty.dbgw.policy.api.PolicyDecision
import com.github.l34130.netty.dbgw.policy.api.config.ManifestMapper
import com.github.l34130.netty.dbgw.policy.api.config.ResourceFactory
import com.github.l34130.netty.dbgw.policy.api.config.ResourceRegistry
import com.github.l34130.netty.dbgw.policy.api.database.DatabaseAuthenticationEvent
import com.github.l34130.netty.dbgw.policy.api.database.DatabaseContext
import com.github.l34130.netty.dbgw.policy.api.database.DatabasePolicyInterceptor
import com.github.l34130.netty.dbgw.policy.api.database.query.DatabaseQueryContext
import java.io.File
import java.util.ServiceLoader
import kotlin.reflect.full.isSuperclassOf

class PolicyEngine(
    val policyChain: DatabasePolicyChain<DatabasePolicyInterceptor> = DatabasePolicyChain(emptyList()),
) {
    fun evaluateAuthenticationPolicy(
        ctx: DatabaseContext,
        evt: DatabaseAuthenticationEvent,
    ): PolicyDecision = policyChain.onAuthentication(ctx, evt)

    fun evaluateQueryPolicy(ctx: DatabaseQueryContext): PolicyDecision = policyChain.onQuery(ctx)

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

            val policies =
                if (file.isFile) {
                    require(file.extension == "yaml" || file.extension == "yml") {
                        "Expected a YAML file, but got: ${file.extension}"
                    }

                    loadFromFile(file)
                } else {
                    val policies = mutableListOf<DatabasePolicyInterceptor>()
                    file.walk().forEach { file ->
                        if (file.isFile && file.extension == "yaml" || file.extension == "yml") {
                            policies.addAll(loadFromFile(file))
                        }
                    }
                    policies
                }

            return PolicyEngine(DatabasePolicyChain(policies))
        }

        private fun loadFromFile(file: File): List<DatabasePolicyInterceptor> {
            require(!file.isDirectory) { "Expected a file, but got a directory: $file" }
            require(file.exists()) { "Policy file does not exist: $file" }
            require(file.extension == "yaml") { "Expected a YAML file, but got: ${file.extension}" }

            val manifests =
                ManifestMapper
                    .readValues(file)
                    .map { it.groupVersionKind() to it }

            val queryPolicies = mutableListOf<DatabasePolicyInterceptor>()
            ServiceLoader.load(ResourceFactory::class.java).forEach { factory ->
                for ((gvk, manifest) in manifests) {
                    if (DatabasePolicyInterceptor::class.isSuperclassOf(factory.type()) && factory.isApplicable(gvk)) {
                        queryPolicies.add(factory.create(manifest.spec) as DatabasePolicyInterceptor)
                    }
                }
            }
            return queryPolicies
        }
    }
}
