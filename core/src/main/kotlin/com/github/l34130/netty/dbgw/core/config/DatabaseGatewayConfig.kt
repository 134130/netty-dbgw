package com.github.l34130.netty.dbgw.core.config

import com.github.l34130.netty.dbgw.core.policy.PolicyEngine
import com.github.l34130.netty.dbgw.policy.api.Resource
import java.io.File
import kotlin.reflect.full.findAnnotation

data class DatabaseGatewayConfig(
    val listenPort: Int,
    val upstreamHost: String,
    val upstreamPort: Int,
    val upstreamDatabaseType: UpstreamDatabaseType,
    val authenticationOverride: Authentication?,
    val policyFile: String? = null,
) {
    val policyEngine: PolicyEngine = policyFile?.let { PolicyEngine.loadFromManifest(File(it)) } ?: PolicyEngine()

    override fun toString(): String =
        buildString {
            appendLine()
            appendLine("    0.0.0.0:$listenPort -> $upstreamHost:$upstreamPort ($upstreamDatabaseType)")
            if (authenticationOverride != null) {
                appendLine("    Authentication Override: ${authenticationOverride.username}")
            } else {
                appendLine("    Authentication Override: None (user input will be used)")
            }
            appendLine("    Policy File: ${policyFile ?: "None"}")
            if (policyFile != null) {
                appendLine("    Policy Engine: ${policyEngine.policies.size} policies loaded")
                for ((index, policy) in policyEngine.policies.withIndex()) {
                    append("        Policy #$index: ")
                    val resourceInfo = policy::class.findAnnotation<Resource>()
                    if (resourceInfo != null) {
                        appendLine("${resourceInfo.group}/${resourceInfo.version}, Kind=${resourceInfo.kind}")
                    } else {
                        appendLine("No Resource annotation")
                    }
                }
            }
        }

    class Wrapper(
        val gateway: DatabaseGatewayConfig,
    )

    enum class UpstreamDatabaseType {
        MYSQL,
        POSTGRESQL,
    }

    data class Authentication(
        val username: String,
        val password: String,
    )
}
