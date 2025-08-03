package com.github.l34130.netty.dbgw.core.config

data class DatabaseGatewayConfig(
    val listenPort: Int,
    val upstreamHost: String,
    val upstreamPort: Int,
    val upstreamDatabaseType: UpstreamDatabaseType,
    val authenticationOverride: Authentication?,
    val policyFile: String? = null,
) {
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
