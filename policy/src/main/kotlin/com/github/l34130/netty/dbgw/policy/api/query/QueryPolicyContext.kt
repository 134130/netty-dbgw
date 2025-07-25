package com.github.l34130.netty.dbgw.policy.api.query

import com.github.l34130.netty.dbgw.policy.api.ClientInfo
import com.github.l34130.netty.dbgw.policy.api.ConnectionInfo
import com.github.l34130.netty.dbgw.policy.api.PolicyContext
import com.github.l34130.netty.dbgw.policy.api.SessionInfo

class QueryPolicyContext(
    clientInfo: ClientInfo,
    connectionInfo: ConnectionInfo,
    sessionInfo: SessionInfo,
    attributes: MutableMap<String, Any> = mutableMapOf(),
) : PolicyContext(
        clientInfo = clientInfo,
        connectionInfo = connectionInfo,
        sessionInfo = sessionInfo,
        attributes = attributes,
    )
