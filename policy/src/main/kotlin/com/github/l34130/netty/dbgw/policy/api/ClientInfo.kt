package com.github.l34130.netty.dbgw.policy.api

data class ClientInfo(
    val sourceIps: List<String>,
    val userAgent: String? = null,
    val applicationName: String? = null,
)
