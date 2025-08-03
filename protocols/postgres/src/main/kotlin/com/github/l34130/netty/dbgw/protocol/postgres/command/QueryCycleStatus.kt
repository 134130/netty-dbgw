package com.github.l34130.netty.dbgw.protocol.postgres.command

import com.github.l34130.netty.dbgw.common.sql.ColumnDefinition
import com.github.l34130.netty.dbgw.core.BusinessLogicAware
import com.github.l34130.netty.dbgw.core.GatewayState
import com.github.l34130.netty.dbgw.core.MessageAction
import com.github.l34130.netty.dbgw.core.databaseCtx
import com.github.l34130.netty.dbgw.core.databasePolicyChain
import com.github.l34130.netty.dbgw.policy.api.PolicyDecision
import com.github.l34130.netty.dbgw.policy.api.database.query.withQuery
import com.github.l34130.netty.dbgw.policy.api.database.query.withResultRow
import com.github.l34130.netty.dbgw.protocol.postgres.Message
import com.github.l34130.netty.dbgw.protocol.postgres.message.ErrorResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.channel.ChannelHandlerContext

// TODO: Handle the state more restrictively, e.g., by checking if the query is in progress or not
class QueryCycleStatus :
    GatewayState<Message, Message>(),
    BusinessLogicAware {
    private val rowDescriptionFields = mutableListOf<RowDescription.Field>()
    private val columnDefinitions: List<ColumnDefinition> by lazy {
        rowDescriptionFields.map { it.toColumnDefinition() }
    }

    override fun onFrontendMessage(
        ctx: ChannelHandlerContext,
        msg: Message,
    ): StateResult =
        when (msg.type) {
            Query.TYPE -> {
                val query = Query.readFrom(msg)
                logger.debug { "Query: $query" }

                ctx.databasePolicyChain()?.let { chain ->
                    val result = chain.onQuery(ctx.databaseCtx()!!.withQuery(query.query))
                    // TODO: Intercept the message and send an error response
                    if (result is PolicyDecision.Deny) {
                        StateResult(
                            nextState = QueryCycleStatus(),
                            action =
                                MessageAction.Terminate(
                                    reason = "Query policy violation: ${result.reason}",
                                ),
                        )
                    } else {
                        null
                    }
                } ?: StateResult(
                    nextState = this,
                    action = MessageAction.Forward,
                )
            }
            Parse.TYPE -> {
                val parse = Parse.readFrom(msg)
                logger.debug { "Parse: $parse" }

                ctx.databasePolicyChain()?.let { chain ->
                    val result = chain.onQuery(ctx.databaseCtx()!!.withQuery(parse.query))
                    // TODO: Intercept the message and send an error response
                    if (result is PolicyDecision.Deny) {
                        StateResult(
                            nextState = QueryCycleStatus(),
                            action =
                                MessageAction.Terminate(
                                    reason = "Query policy violation: ${result.reason}",
                                ),
                        )
                    } else {
                        null
                    }
                } ?: StateResult(
                    nextState = this,
                    action = MessageAction.Forward,
                )
            }
            Bind.TYPE -> {
                val bind = Bind.readFrom(msg)
                logger.trace { "Bind: $bind" }
                StateResult(
                    nextState = this,
                    action = MessageAction.Forward,
                )
            }
            Execute.TYPE -> {
                val execute = Execute.readFrom(msg)
                logger.trace { "Execute: $execute" }
                StateResult(
                    nextState = this,
                    action = MessageAction.Forward,
                )
            }
            Sync.TYPE -> {
                logger.trace { "Sync message received" }
                StateResult(
                    nextState = this,
                    action = MessageAction.Forward,
                )
            }
            Describe.TYPE -> {
                val describe = Describe.readFrom(msg)
                logger.trace { "Describe: $describe" }
                StateResult(
                    nextState = this,
                    action = MessageAction.Forward,
                )
            }
            else -> TODO("Unsupported command type: ${msg.type}")
        }

    override fun onBackendMessage(
        ctx: ChannelHandlerContext,
        msg: Message,
    ): StateResult {
        when (msg.type) {
            ParseComplete.TYPE -> {
                logger.trace { "Parse complete" }
                return StateResult(
                    nextState = this,
                    action = MessageAction.Forward,
                )
            }
            BindComplete.TYPE -> {
                logger.trace { "Bind complete" }
                return StateResult(
                    nextState = this,
                    action = MessageAction.Forward,
                )
            }
            ErrorResponse.TYPE -> {
                val errorResponse = ErrorResponse.readFrom(msg)
                logger.trace { "Error response: $errorResponse" }
                return StateResult(
                    nextState = this,
                    action = MessageAction.Forward,
                )
            }
            Close.TYPE -> {
                val closeMessage = Close.readFrom(msg)
                logger.trace { "Close: $closeMessage" }
                return StateResult(
                    nextState = this,
                    action = MessageAction.Forward,
                )
            }
            ReadyForQuery.TYPE -> {
                val readyForQuery = ReadyForQuery.readFrom(msg)
                logger.trace { "Ready for query: $readyForQuery" }
                return StateResult(
                    nextState = QueryCycleStatus(),
                    action = MessageAction.Forward,
                )
            }
            ParameterStatus.TYPE -> {
                val parameterStatus = ParameterStatus.readFrom(msg)
                logger.trace { "Parameter status: $parameterStatus" }
                return StateResult(
                    nextState = this,
                    action = MessageAction.Forward,
                )
            }
            DataRow.TYPE -> {
                val dataRow = DataRow.readFrom(msg)
                logger.trace { "Data row: $dataRow" }

                val resultRowCtx = ctx.databaseCtx()!!.withResultRow(columnDefinitions, dataRow.columnValues)
                ctx.databasePolicyChain()!!.onResultRow(resultRowCtx)
                return StateResult(
                    nextState = this,
                    action =
                        MessageAction.Transform(
                            DataRow(
                                columnValues = resultRowCtx.resultRow(),
                            ).asMessage(),
                        ),
                )
            }
            RowDescription.TYPE -> {
                val rowDescription = RowDescription.readFrom(msg)
                logger.trace { "Row description: $rowDescription" }

                rowDescriptionFields.addAll(rowDescription.fields)

                return StateResult(
                    nextState = this,
                    action = MessageAction.Forward,
                )
            }
            NoData.TYPE -> {
                logger.trace { "No data message received" }
                return StateResult(
                    nextState = this,
                    action = MessageAction.Forward,
                )
            }
            EmptyQueryResponse.TYPE -> {
                logger.trace { "Empty query response received" }
                return StateResult(
                    nextState = this,
                    action = MessageAction.Forward,
                )
            }
            else -> error("Unexpected backend message type '${msg.type}'")
        }
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}
