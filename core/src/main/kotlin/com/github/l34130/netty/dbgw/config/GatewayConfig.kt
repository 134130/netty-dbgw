package com.github.l34130.netty.dbgw.config

data class GatewayConfig(
    val listenPort: Int,
    val upstreamHost: String,
    val upstreamPort: Int,
    val upstreamDatabaseType: UpstreamDatabaseType,
    val restrictedSqlStatements: List<String>,
    val authentication: Authentication?,
) {
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
            if (authentication != null) {
                appendLine("    Authentication: ${authentication.username}")
            } else {
                appendLine("    Authentication: None (user input will be bypassed)")
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
