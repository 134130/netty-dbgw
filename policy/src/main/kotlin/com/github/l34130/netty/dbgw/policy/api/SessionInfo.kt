package com.github.l34130.netty.dbgw.policy.api

/**
 * Represents session information for a gateway system.
 */
data class SessionInfo(
    var sessionId: String,
    var userId: String? = null,
    var username: String? = null,
)
