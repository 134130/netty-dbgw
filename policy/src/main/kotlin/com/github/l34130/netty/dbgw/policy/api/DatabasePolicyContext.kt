package com.github.l34130.netty.dbgw.policy.api

// TODO: jq able
open class DatabasePolicyContext(
    val clientInfo: ClientInfo,
    val connectionInfo: DatabaseConnectionInfo,
    val sessionInfo: SessionInfo,
    val attributes: MutableMap<String, Any> = mutableMapOf(),
)
