package com.github.l34130.netty.dbgw.policy.api

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

object PolicyRegistry {
    private val logger = KotlinLogging.logger { }
    private val metadataStore = mutableMapOf<String, PolicyMetadata>()

    init {
        // Register all policies at startup
        registerAllPolicies()
    }

    fun register(
        policyClass: KClass<*>,
        policyAnnotation: Policy? = null,
    ) {
        val policyAnnotation =
            policyAnnotation ?: policyClass.findAnnotation<Policy>()
                ?: error("Policy class ${policyClass.simpleName} does not have @Policy annotation")

        try {
            val metadata = PolicyMetadata.from(policyAnnotation)
            metadataStore[metadata.key()] = metadata
        } catch (e: IllegalArgumentException) {
            logger.error(e) { "Failed to register policy class: ${policyClass.simpleName}" }
        }
    }

    fun get(key: String): PolicyMetadata? = metadataStore[key]

    fun find(
        group: String,
        version: String,
        kind: String,
    ): PolicyMetadata? = metadataStore.values.find { it.isApplicable(group, version, kind) }

    fun all(): Collection<PolicyMetadata> = metadataStore.values

    private fun registerAllPolicies() {
    }
}
