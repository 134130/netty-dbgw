package com.github.l34130.netty.dbgw.policy.api.database.query

import com.github.l34130.netty.dbgw.common.sql.ColumnDefinition
import com.github.l34130.netty.dbgw.policy.api.ClientInfo
import com.github.l34130.netty.dbgw.policy.api.SessionInfo
import com.github.l34130.netty.dbgw.policy.api.database.DatabaseConnectionInfo
import com.github.l34130.netty.dbgw.policy.api.database.DatabaseContext

class DatabaseResultRowContext internal constructor(
    clientInfo: ClientInfo,
    connectionInfo: DatabaseConnectionInfo,
    sessionInfo: SessionInfo,
    attributes: MutableMap<String, Any>,
    val columnDefinitions: List<ColumnDefinition>,
    val resultRow: List<String?>,
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

    fun addRowProcessorFactory(
        processorFactory: (
            columnDefinitions: List<ColumnDefinition>,
            originalRow: List<String?>,
        ) -> (Sequence<String?>) -> Sequence<String?>,
    ) {
        processors.add(processorFactory(columnDefinitions, resultRow))
    }
}

fun DatabaseContext.withResultRow(
    columnDefinitions: List<ColumnDefinition>,
    resultRow: List<String?>,
): DatabaseResultRowContext =
    DatabaseResultRowContext(
        clientInfo = this.clientInfo,
        connectionInfo = this.connectionInfo,
        sessionInfo = this.sessionInfo,
        attributes = this.attributes,
        columnDefinitions = columnDefinitions,
        resultRow = resultRow,
    )
