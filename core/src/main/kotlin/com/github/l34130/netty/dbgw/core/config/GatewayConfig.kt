package com.github.l34130.netty.dbgw.core.config

import com.github.l34130.netty.dbgw.core.policy.PolicyEngine

data class GatewayConfig(
    val listenPort: Int,
    val upstreamHost: String,
    val upstreamPort: Int,
    val upstreamDatabaseType: UpstreamDatabaseType,
    @Deprecated("")
    val restrictedSqlStatements: List<String>,
    val authenticationOverride: Authentication?,
) {
    // TODO: Remove this once the policy engine is fully integrated
    val policyEngine =
        PolicyEngine().apply {
            init()
        }

    override fun toString(): String =
        buildString {
            appendLine()
            appendLine("    0.0.0.0:$listenPort -> $upstreamHost:$upstreamPort ($upstreamDatabaseType)")
            if (restrictedSqlStatements.isNotEmpty()) {
                appendLine("    Restricted SQL Statements:")
                restrictedSqlStatements.forEach { sql ->
                    appendLine("      - $sql")
                }
            }
            if (authenticationOverride != null) {
                appendLine("    Authentication Override: ${authenticationOverride.username}")
            } else {
                appendLine("    Authentication Override: None (user input will be used)")
            }
        }

    class Wrapper(
        val gateway: GatewayConfig,
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
