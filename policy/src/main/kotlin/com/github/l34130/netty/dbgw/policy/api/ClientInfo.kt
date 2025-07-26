package com.github.l34130.netty.dbgw.policy.api

/**
 * Represents client information connected to the gateway.
 */
data class ClientInfo(
    var sourceIps: List<String>,
    var userAgent: String? = null,
    var applicationName: String? = null,
)
