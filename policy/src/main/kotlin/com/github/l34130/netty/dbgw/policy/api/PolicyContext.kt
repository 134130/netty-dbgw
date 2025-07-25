package com.github.l34130.netty.dbgw.policy.api

// TODO: jq able
open class PolicyContext(
    val clientInfo: ClientInfo,
    val connectionInfo: ConnectionInfo,
    val sessionInfo: SessionInfo,
    val attributes: MutableMap<String, Any> = mutableMapOf(),
)
