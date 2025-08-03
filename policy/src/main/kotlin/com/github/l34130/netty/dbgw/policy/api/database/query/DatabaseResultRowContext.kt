package com.github.l34130.netty.dbgw.policy.api.database.query

import com.github.l34130.netty.dbgw.policy.api.ClientInfo
import com.github.l34130.netty.dbgw.policy.api.SessionInfo
import com.github.l34130.netty.dbgw.policy.api.database.DatabaseConnectionInfo
import com.github.l34130.netty.dbgw.policy.api.database.DatabaseContext

class DatabaseResultRowContext internal constructor(
    clientInfo: ClientInfo,
    connectionInfo: DatabaseConnectionInfo,
    sessionInfo: SessionInfo,
    attributes: MutableMap<String, Any>,
    private val resultRow: List<String?>,
) : DatabaseContext(
        clientInfo,
        connectionInfo,
        sessionInfo,
        attributes,
    ) {
    private val processors = mutableListOf<(Sequence<String?>) -> Sequence<String?>>()

    fun resultRow(): List<String?> =
        processors
            .fold(resultRow.asSequence()) { acc, processor ->
                processor(acc)
            }.toList()

    fun addRowProcessorFactory(processorFactory: (originalRow: List<String?>) -> (Sequence<String?>) -> Sequence<String?>) {
        processors.add(processorFactory(resultRow))
    }
}

fun DatabaseContext.withResultRow(resultRow: List<String?>): DatabaseResultRowContext =
    DatabaseResultRowContext(
        clientInfo = this.clientInfo,
        connectionInfo = this.connectionInfo,
        sessionInfo = this.sessionInfo,
        attributes = this.attributes,
        resultRow = resultRow,
    )
