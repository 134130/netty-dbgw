package com.github.l34130.netty.dbgw.policy.api.database.query

import com.github.l34130.netty.dbgw.policy.api.ClientInfo
import com.github.l34130.netty.dbgw.policy.api.SessionInfo
import com.github.l34130.netty.dbgw.policy.api.database.DatabaseConnectionInfo
import com.github.l34130.netty.dbgw.policy.api.database.DatabaseContext

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
