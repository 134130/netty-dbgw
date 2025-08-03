package com.github.l34130.netty.dbgw.policy.api.database.query

import com.github.l34130.netty.dbgw.policy.api.ClientInfo
import com.github.l34130.netty.dbgw.policy.api.SessionInfo
import com.github.l34130.netty.dbgw.policy.api.database.DatabaseConnectionInfo
import com.github.l34130.netty.dbgw.policy.api.database.DatabaseContext

class DatabaseResultSetContext internal constructor(
    clientInfo: ClientInfo,
    connectionInfo: DatabaseConnectionInfo,
    sessionInfo: SessionInfo,
    attributes: MutableMap<String, Any>,
    private var resultSet: Sequence<Sequence<String>>,
) : DatabaseContext(
        clientInfo,
        connectionInfo,
        sessionInfo,
        attributes,
    ) {
    private val processors = mutableListOf<(Sequence<String>) -> Sequence<String>>()

    fun addResultSetProcessor(transform: (Sequence<String>) -> Sequence<String>) {
        processors.add(transform)
    }
}

fun DatabaseContext.withResultSet(resultSet: Sequence<Sequence<String>>): DatabaseResultSetContext =
    DatabaseResultSetContext(
        clientInfo = this.clientInfo,
        connectionInfo = this.connectionInfo,
        sessionInfo = this.sessionInfo,
        attributes = this.attributes,
        resultSet = resultSet,
    )
