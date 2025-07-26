package com.github.l34130.netty.dbgw.policy.api.query

import com.github.l34130.netty.dbgw.policy.api.ClientInfo
import com.github.l34130.netty.dbgw.policy.api.DatabaseConnectionInfo
import com.github.l34130.netty.dbgw.policy.api.DatabaseContext
import com.github.l34130.netty.dbgw.policy.api.SessionInfo

class DatabaseQueryContext internal constructor(
    clientInfo: ClientInfo,
    connectionInfo: DatabaseConnectionInfo,
    sessionInfo: SessionInfo,
    attributes: MutableMap<String, Any> = mutableMapOf(),
    val query: String,
) : DatabaseContext(
        clientInfo = clientInfo,
        connectionInfo = connectionInfo,
        sessionInfo = sessionInfo,
        attributes = attributes,
    )

fun DatabaseContext.withQuery(query: String): DatabaseQueryContext =
    DatabaseQueryContext(
        clientInfo = this.clientInfo,
        connectionInfo = this.connectionInfo,
        sessionInfo = this.sessionInfo,
        attributes = this.attributes,
        query = query,
    )
