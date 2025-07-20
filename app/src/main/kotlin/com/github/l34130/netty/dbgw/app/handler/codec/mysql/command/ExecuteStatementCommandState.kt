package com.github.l34130.netty.dbgw.app.handler.codec.mysql.command

import com.github.l34130.netty.dbgw.app.handler.codec.mysql.Bitmap
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.GatewayState
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.Packet
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.capabilities
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.constant.CapabilityFlag
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.constant.CursorTypeFlag
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.constant.MySqlFieldType
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.downstream
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.preparedStatements
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.readFixedLengthInteger
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.readFixedLengthString
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.readLenEncInteger
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.readLenEncString
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.upstream
import com.github.l34130.netty.dbgw.utils.netty.peek
import com.github.l34130.netty.dbgw.utils.toEnumSet
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.channel.ChannelHandlerContext

// https://dev.mysql.com/doc/dev/mysql-server/latest/page_protocol_com_stmt_execute.html
class ExecuteStatementCommandState : GatewayState {
    private var requested = false

    override fun onDownstreamPacket(
        ctx: ChannelHandlerContext,
        packet: Packet,
    ): GatewayState {
        check(!requested) { "Duplicate COM_STMT_EXECUTE request received" }
        requested = true

        val payload = packet.payload
        payload.markReaderIndex()

        val status = payload.readFixedLengthInteger(1).toUInt()
        val commandType = CommandPhaseState.CommandType.from(status)
        require(commandType == CommandPhaseState.CommandType.COM_STMT_EXECUTE) {
            "Expected COM_STMT_EXECUTE command type, but received: ${commandType ?: "0x${status.toString(16)}"}"
        }

        val statementId = payload.readFixedLengthInteger(4).toUInt()
        val flags = payload.readFixedLengthInteger(1).toEnumSet<CursorTypeFlag>()
        val iterationCount = payload.readFixedLengthInteger(4) // always 1

        val preparedStatement =
            ctx.preparedStatements()[statementId]
                ?: error("Prepared statement with ID $statementId not found in the context")

        logger.trace { "Executing prepared statement with ID: $statementId, flags: $flags, iteration count: $iterationCount" }
        logger.trace { "Prepared Statement: $preparedStatement" }

        val clientQueryAttributes = ctx.capabilities().contains(CapabilityFlag.CLIENT_QUERY_ATTRIBUTES)

        var parameterCount = 0UL
        if (preparedStatement.parameterDefinitions.isNotEmpty() ||
            clientQueryAttributes &&
            flags.contains(CursorTypeFlag.PARAMETER_COUNT_AVAILABLE)
        ) {
            if (clientQueryAttributes) {
                parameterCount = payload.readLenEncInteger()
            }

            if (parameterCount > 0UL) {
                val nullBitmapLength = (parameterCount + 7UL) / 8UL
                payload.skipBytes(nullBitmapLength.toInt()) // skip null bitmap

                val parameters = mutableListOf<Triple<MySqlFieldType?, String?, Any?>>()
                (0UL until parameterCount).forEach { i ->
                    var parameterType: MySqlFieldType? = null
                    var parameterName: String? = null

                    val newParamsBindFlag = payload.readFixedLengthInteger(1) == 1UL
                    if (newParamsBindFlag) {
                        // Read parameter types and values
                        for (i in 0UL until parameterCount) {
                            parameterType = MySqlFieldType.of(payload.readFixedLengthInteger(2).toInt())
                            if (clientQueryAttributes) {
                                parameterName = payload.readLenEncString().toString(Charsets.UTF_8)
                            }
                        }
                    }
                    val parameterValue = payload.readLenEncString().toString(Charsets.UTF_8)

                    parameters.add(Triple(parameterType, parameterName, parameterValue))
                }

                logger.trace { "Parameters: $parameters" }
            }
        }

        // Forward the packet to the upstream handler
        payload.resetReaderIndex()
        ctx.upstream().writeAndFlush(packet)
        return this
    }

    override fun onUpstreamPacket(
        ctx: ChannelHandlerContext,
        packet: Packet,
    ): GatewayState {
        check(requested) { "COM_STMT_EXECUTE response received without a prior request" }

        logger.trace { "Processing COM_STMT_EXECUTE Response" }

        val payload = packet.payload
        payload.markReaderIndex()

        if (packet.isErrorPacket()) {
            logger.trace {
                val errPacket = payload.peek { Packet.Error.readFrom(it, ctx.capabilities().enumSet()) }
                "Received COM_STMT_EXECUTE error response: $errPacket"
            }

            payload.resetReaderIndex()
            ctx.downstream().writeAndFlush(packet)
            return CommandPhaseState()
        }

        if (packet.isOkPacket()) {
            logger.trace { "Received COM_STMT_EXECUTE OK response" }
            packet.payload.resetReaderIndex()

            payload.resetReaderIndex()
            ctx.downstream().writeAndFlush(packet)
            return CommandPhaseState()
        }

        return BinaryProtocolResultsetState().onUpstreamPacket(ctx, packet)
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }

    private class BinaryProtocolResultsetState : GatewayState {
        private var state: State = State.INITIAL
        private var columnCount: ULong = 0UL
        private val columnDefinitions = mutableListOf<ColumnDefinition41>()

        override fun onUpstreamPacket(
            ctx: ChannelHandlerContext,
            packet: Packet,
        ): GatewayState =
            when (state) {
                State.INITIAL -> handleInitialState(ctx, packet)
                State.COLUMN_DEFINITION -> handleColumnDefinitionState(ctx, packet)
                State.RESULTSET_ROW -> handleResultsetRow(ctx, packet)
            }

        private fun handleInitialState(
            ctx: ChannelHandlerContext,
            packet: Packet,
        ): GatewayState {
            val payload = packet.payload
            payload.markReaderIndex()

            columnCount = payload.readLenEncInteger()
            logger.trace { "Column Count: $columnCount" }

            check(columnCount > 0UL) { "Column count must be greater than 0, but was $columnCount" }

            state = State.COLUMN_DEFINITION

            payload.resetReaderIndex()
            ctx.downstream().writeAndFlush(packet)
            return this
        }

        private fun handleColumnDefinitionState(
            ctx: ChannelHandlerContext,
            packet: Packet,
        ): GatewayState {
            val payload = packet.payload
            payload.markReaderIndex()

            val columnDef = ColumnDefinition41.readFrom(packet.payload)
            logger.trace { "Received Column Definition: $columnDef" }

            columnDefinitions.add(columnDef)

            state =
                if (columnDefinitions.size == columnCount.toInt()) {
                    State.RESULTSET_ROW
                } else {
                    State.COLUMN_DEFINITION
                }

            payload.resetReaderIndex()
            ctx.downstream().writeAndFlush(packet)
            return this
        }

        private fun handleResultsetRow(
            ctx: ChannelHandlerContext,
            packet: Packet,
        ): GatewayState {
            val payload = packet.payload
            payload.markReaderIndex()

            // TODO: Handle OK Packet if CLIENT_DEPRECATE_EOF is set
            if (packet.isEofPacket()) {
                logger.trace {
                    val packet = Packet.Eof.readFrom(payload, ctx.capabilities().enumSet())
                    "Received Resultset EOF: $packet"
                }

                payload.resetReaderIndex()
                ctx.downstream().writeAndFlush(packet)
                return CommandPhaseState()
            }

            val packetHeader = payload.readFixedLengthInteger(1).toUInt()
            check(packetHeader == 0x00U) { "Expected column definition packet header 0x00, but received: 0x${packetHeader.toString(16)}" }

            val nullBitmapLength = (columnCount + 7U + 2U) / 8U
            val nullBitmap = Bitmap.readFrom(payload, nullBitmapLength.toInt())
            val values =
                (0UL until columnCount).map { i ->
                    if (nullBitmap.get(i.toInt())) {
                        null // Column value is NULL
                    } else {
                        val columnLength = columnDefinitions[i.toInt()].lengthOfFixedLengthFields
                        payload.readFixedLengthString(columnLength)
                    }
                }

            logger.trace { "Received Resultset Row: $values" }

            payload.resetReaderIndex()
            ctx.downstream().writeAndFlush(packet)
            return this
        }

        companion object {
            private val logger = KotlinLogging.logger { }
        }

        private enum class State {
            INITIAL,
            COLUMN_DEFINITION,
            RESULTSET_ROW,
        }
    }
}
