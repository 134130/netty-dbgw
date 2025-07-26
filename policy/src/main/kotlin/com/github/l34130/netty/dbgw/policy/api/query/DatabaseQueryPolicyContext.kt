package com.github.l34130.netty.dbgw.policy.api.query

import com.github.l34130.netty.dbgw.policy.api.ClientInfo
import com.github.l34130.netty.dbgw.policy.api.DatabaseConnectionInfo
import com.github.l34130.netty.dbgw.policy.api.DatabasePolicyContext
import com.github.l34130.netty.dbgw.policy.api.SessionInfo

class DatabaseQueryPolicyContext(
    clientInfo: ClientInfo,
    connectionInfo: DatabaseConnectionInfo,
    sessionInfo: SessionInfo,
    attributes: MutableMap<String, Any> = mutableMapOf(),
) : DatabasePolicyContext(
        clientInfo = clientInfo,
        connectionInfo = connectionInfo,
        sessionInfo = sessionInfo,
        attributes = attributes,
    )
