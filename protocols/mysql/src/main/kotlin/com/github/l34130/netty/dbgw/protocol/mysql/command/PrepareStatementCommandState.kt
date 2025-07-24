package com.github.l34130.netty.dbgw.protocol.mysql.command

import com.github.l34130.netty.dbgw.core.MessageAction
import com.github.l34130.netty.dbgw.core.utils.netty.peek
import com.github.l34130.netty.dbgw.protocol.mysql.MySqlGatewayState
import com.github.l34130.netty.dbgw.protocol.mysql.Packet
import com.github.l34130.netty.dbgw.protocol.mysql.capabilities
import com.github.l34130.netty.dbgw.protocol.mysql.constant.CapabilityFlag
import com.github.l34130.netty.dbgw.protocol.mysql.preparedStatements
import com.github.l34130.netty.dbgw.protocol.mysql.readFixedLengthInteger
import com.github.l34130.netty.dbgw.protocol.mysql.readRestOfPacketString
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.channel.ChannelHandlerContext

// https://dev.mysql.com/doc/dev/mysql-server/latest/page_protocol_com_stmt_prepare.html
internal class PrepareStatementCommandState : MySqlGatewayState() {
    private var requested = false
    private var responseState: ResponseState = ResponseState.OK

    private var numColumns: UShort = 0U
    private var numParams: UShort = 0U
    private var metadataFollows: Boolean = false

    private var parameterDefinitionCount: UShort = 0U
    private var columnDefinitionCount: UShort = 0U

    private val preparedStatementBuilder = PreparedStatement.builder()

    override fun onDownstreamMessage(
        ctx: ChannelHandlerContext,
        msg: Packet,
    ): StateResult {
        check(!requested) { "Duplicate COM_STMT_PREPARE request received." }
        requested = true

        val payload = msg.payload
        val query = payload.readRestOfPacketString().toString(Charsets.UTF_8)
        logger.trace { "Received COM_STMT_PREPARE request for query: $query" }

        preparedStatementBuilder.query(query)

        return StateResult(
            nextState = this,
            action = MessageAction.Forward,
        )
    }

    override fun onUpstreamMessage(
        ctx: ChannelHandlerContext,
        msg: Packet,
    ): StateResult {
        check(requested) { "COM_STMT_PREPARE response received without a prior request" }

        logger.trace { "COM_STMT_PREPARE Response state: $responseState" }

        val nextState =
            when (responseState) {
                ResponseState.OK -> handleFirstResponse(ctx, msg)
                ResponseState.PARAMS -> handleParamsResponse(ctx, msg)
                ResponseState.PARAMS_EOF -> handleEofPacket(ctx, msg)
                ResponseState.COLUMNS -> handleColumnsResponse(ctx, msg)
                ResponseState.COLUMNS_EOF -> handleEofPacket(ctx, msg)
            }

        if (nextState.nextState is CommandPhaseState) {
            val preparedStatement = preparedStatementBuilder.build()
            ctx.preparedStatements().put(preparedStatement.statementId, preparedStatement)
        }

        return nextState
    }

    private fun handleFirstResponse(
        ctx: ChannelHandlerContext,
        packet: Packet,
    ): StateResult {
        val payload = packet.payload
        if (packet.isErrorPacket()) {
            logger.trace {
                val errPacket = payload.peek { Packet.Error.readFrom(it, ctx.capabilities().enumSet()) }
                "Received COM_STMT_PREPARE error response: $errPacket"
            }

            return StateResult(
                nextState = CommandPhaseState(),
                action = MessageAction.Forward,
            )
        }

        // Read the first response packet
        payload.skipBytes(1) // Skip the first status byte
        val statementId = payload.readFixedLengthInteger(4).toUInt()
        this.numColumns = payload.readFixedLengthInteger(2).toUShort()
        this.numParams = payload.readFixedLengthInteger(2).toUShort()
        payload.skipBytes(1) // Skip reserved byte

        var warningCount = 0
        if (payload.readableBytes() > 0) {
            warningCount = payload.readFixedLengthInteger(2).toInt()
            if (ctx.capabilities().contains(CapabilityFlag.CLIENT_OPTIONAL_RESULTSET_METADATA)) {
                this.metadataFollows = payload.readFixedLengthInteger(1).toInt() == 1
            }
        }

        preparedStatementBuilder.statementId(statementId)

        logger.trace {
            "COM_STMT_PREPARE Response: statementId=$statementId, numColumns=$numColumns, numParams=$numParams, warningCount=$warningCount, metadataFollows=$metadataFollows"
        }

        val nextState =
            if (numParams > 0U || metadataFollows) {
                responseState = ResponseState.PARAMS
                this // Wait for parameter metadata packets
            } else if (numColumns > 0U) {
                responseState = ResponseState.COLUMNS
                this // Wait for column metadata packets
            } else {
                CommandPhaseState() // No parameters, proceed to command phase
            }
        return StateResult(
            nextState = nextState,
            action = MessageAction.Forward,
        )
    }

    private fun handleParamsResponse(
        ctx: ChannelHandlerContext,
        packet: Packet,
    ): StateResult {
        check(++parameterDefinitionCount <= numParams) {
            "Received more parameter definitions than expected: $parameterDefinitionCount > $numParams"
        }

        val payload = packet.payload
        val columnDef = ColumnDefinition41.readFrom(payload)
        preparedStatementBuilder.addParameterDefinition(columnDef)
        logger.trace { "Received parameter definition: $columnDef" }

        val nextState =
            if (parameterDefinitionCount == numParams) {
                if (!ctx.capabilities().contains(CapabilityFlag.CLIENT_DEPRECATE_EOF)) {
                    responseState = ResponseState.PARAMS_EOF
                    this // Wait for EOF packet
                } else if (numColumns > 0U || metadataFollows) {
                    responseState = ResponseState.COLUMNS
                    this // Wait for column definitions
                } else {
                    CommandPhaseState() // No columns, return to command phase
                }
            } else {
                this // Wait next parameter definition or EOF packet
            }

        return StateResult(
            nextState = nextState,
            action = MessageAction.Forward,
        )
    }

    private fun handleColumnsResponse(
        ctx: ChannelHandlerContext,
        packet: Packet,
    ): StateResult {
        check(++columnDefinitionCount <= numColumns) {
            "Received more column definitions than expected: $columnDefinitionCount > $numColumns"
        }

        val payload = packet.payload
        val columnDef = ColumnDefinition41.readFrom(payload)
        preparedStatementBuilder.addColumnDefinition(columnDef)
        logger.trace { "Received column definition: $columnDef" }

        val nextState =
            if (columnDefinitionCount == numColumns) {
                if (!ctx.capabilities().contains(CapabilityFlag.CLIENT_DEPRECATE_EOF)) {
                    responseState = ResponseState.COLUMNS_EOF
                    this // Wait for EOF packet
                } else {
                    CommandPhaseState() // All metadata processed, return to command phase
                }
            } else {
                this // Continue to next column definition
            }

        return StateResult(
            nextState = nextState,
            action = MessageAction.Forward,
        )
    }

    private fun handleEofPacket(
        ctx: ChannelHandlerContext,
        packet: Packet,
    ): StateResult {
        check(packet.isEofPacket()) { "Expected EOF packet, but received: $packet" }

        val payload = packet.payload
        val nextState =
            when (responseState) {
                ResponseState.PARAMS_EOF -> {
                    logger.trace {
                        val eofPacket = Packet.Eof.readFrom(payload, ctx.capabilities().enumSet())
                        "Received EOF for parameters: $eofPacket"
                    }

                    if (numColumns > 0U || metadataFollows) {
                        responseState = ResponseState.COLUMNS
                        this // Wait for column definitions
                    } else {
                        CommandPhaseState() // No columns, return to command phase
                    }
                }
                ResponseState.COLUMNS_EOF -> {
                    logger.trace {
                        val eofPacket = Packet.Eof.readFrom(payload, ctx.capabilities().enumSet())
                        "EOF for columns: $eofPacket"
                    }
                    CommandPhaseState() // All metadata processed, return to command phase
                }
                else -> {
                    error("Unexpected EOF packet in state: $responseState")
                }
            }

        return StateResult(
            nextState = nextState,
            action = MessageAction.Forward,
        )
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    private enum class ResponseState {
        OK,
        PARAMS,
        PARAMS_EOF,
        COLUMNS,
        COLUMNS_EOF,
    }
}
