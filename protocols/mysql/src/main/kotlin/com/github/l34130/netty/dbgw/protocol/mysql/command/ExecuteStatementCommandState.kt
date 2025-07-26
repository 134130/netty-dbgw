package com.github.l34130.netty.dbgw.protocol.mysql.command

import com.github.l34130.netty.dbgw.core.MessageAction
import com.github.l34130.netty.dbgw.core.utils.netty.peek
import com.github.l34130.netty.dbgw.core.utils.toEnumSet
import com.github.l34130.netty.dbgw.protocol.mysql.Bitmap
import com.github.l34130.netty.dbgw.protocol.mysql.MySqlGatewayState
import com.github.l34130.netty.dbgw.protocol.mysql.Packet
import com.github.l34130.netty.dbgw.protocol.mysql.capabilities
import com.github.l34130.netty.dbgw.protocol.mysql.constant.CapabilityFlag
import com.github.l34130.netty.dbgw.protocol.mysql.constant.CursorTypeFlag
import com.github.l34130.netty.dbgw.protocol.mysql.constant.MySqlFieldType
import com.github.l34130.netty.dbgw.protocol.mysql.preparedStatements
import com.github.l34130.netty.dbgw.protocol.mysql.readFixedLengthInteger
import com.github.l34130.netty.dbgw.protocol.mysql.readFixedLengthString
import com.github.l34130.netty.dbgw.protocol.mysql.readLenEncInteger
import com.github.l34130.netty.dbgw.protocol.mysql.readLenEncString
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import java.time.LocalDateTime

// https://dev.mysql.com/doc/dev/mysql-server/latest/page_protocol_com_stmt_execute.html
internal class ExecuteStatementCommandState : MySqlGatewayState() {
    private var requested = false

    override fun onFrontendMessage(
        ctx: ChannelHandlerContext,
        msg: Packet,
    ): StateResult {
        check(!requested) { "Duplicate COM_STMT_EXECUTE request received" }
        requested = true

        val payload = msg.payload
        val status = payload.readFixedLengthInteger(1).toUInt()
        val commandType = CommandPhaseState.CommandType.from(status)
        require(commandType == CommandPhaseState.CommandType.COM_STMT_EXECUTE) {
            "Expected COM_STMT_EXECUTE command type, but received: ${commandType ?: "0x${"%02x".format(status).uppercase()}"}"
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
                val nullBitmap = Bitmap.readFrom(payload, nullBitmapLength.toInt())

                val newParamsBindFlag = payload.readFixedLengthInteger(1) == 1UL
                val parameterTypeAndNames: List<Pair<MySqlFieldType, String?>> =
                    if (newParamsBindFlag) {
                        logger.trace { "New params bind flag is set, reading parameter types and names" }

                        // Read parameter types and values
                        (0UL until parameterCount).map { i ->
                            val parameterType = MySqlFieldType.of(payload.readFixedLengthInteger(2).toInt())
                            val parameterName =
                                if (clientQueryAttributes) {
                                    payload.readLenEncString().toString(Charsets.UTF_8)
                                } else {
                                    null
                                }
                            parameterType to parameterName
                        }
                    } else {
                        logger.trace { "New params bind flag is not set, using prepared statement definitions" }
                        preparedStatement.parameterDefinitions.map { it.type to it.name }
                    }

                val parameters =
                    (0UL until parameterCount).map { i ->
                        val parameterType: MySqlFieldType = parameterTypeAndNames[i.toInt()].first
                        val parameterName: String? = parameterTypeAndNames[i.toInt()].second
                        val parameterValue =
                            if (nullBitmap.get(i.toInt())) {
                                null
                            } else {
                                payload.readFieldValue(parameterType)
                            }
                        Triple(parameterType, parameterName, parameterValue)
                    }

                logger.trace { "Parameters: $parameters" }
            }
        }

        // Forward the packet to the backend handler
        return StateResult(
            nextState = this,
            action = MessageAction.Forward,
        )
    }

    override fun onBackendMessage(
        ctx: ChannelHandlerContext,
        msg: Packet,
    ): StateResult {
        check(requested) { "COM_STMT_EXECUTE response received without a prior request" }

        logger.trace { "Processing COM_STMT_EXECUTE Response" }

        val payload = msg.payload
        if (msg.isErrorPacket()) {
            logger.trace {
                val errPacket = payload.peek { Packet.Error.readFrom(it, ctx.capabilities().enumSet()) }
                "Received COM_STMT_EXECUTE error response: $errPacket"
            }

            return StateResult(
                nextState = CommandPhaseState(),
                action = MessageAction.Forward,
            )
        }

        if (msg.isOkPacket()) {
            logger.trace { "Received COM_STMT_EXECUTE OK response" }
            return StateResult(
                nextState = CommandPhaseState(),
                action = MessageAction.Forward,
            )
        }

        // If the response is not an error or OK packet, it must be a result set.
        // The BinaryProtocolResultsetState will handle the result set packets.
        return BinaryProtocolResultsetState().onBackendMessage(ctx, msg)
    }

    companion object {
        private val logger = KotlinLogging.logger { }

        private fun ByteBuf.readFieldValue(type: MySqlFieldType): Any =
            when (type) {
                MySqlFieldType.MYSQL_TYPE_TINY -> this.readFixedLengthInteger(1).toUByte()
                MySqlFieldType.MYSQL_TYPE_SHORT -> this.readFixedLengthInteger(2).toShort()
                MySqlFieldType.MYSQL_TYPE_LONG -> this.readFixedLengthInteger(4).toLong()
                MySqlFieldType.MYSQL_TYPE_FLOAT -> this.readFixedLengthString(4) // IEEE 754 double
                MySqlFieldType.MYSQL_TYPE_DOUBLE -> this.readFixedLengthString(8) // IEEE 754 double
                MySqlFieldType.MYSQL_TYPE_TIMESTAMP,
                MySqlFieldType.MYSQL_TYPE_DATE,
                MySqlFieldType.MYSQL_TYPE_DATETIME,
                -> this.readDateTime()
                MySqlFieldType.MYSQL_TYPE_TIME -> this.readTime()
                MySqlFieldType.MYSQL_TYPE_LONGLONG -> this.readFixedLengthInteger(8)
                MySqlFieldType.MYSQL_TYPE_INT24 -> this.readFixedLengthInteger(4).toUInt()
                MySqlFieldType.MYSQL_TYPE_YEAR -> this.readFixedLengthInteger(2).toShort()
                MySqlFieldType.MYSQL_TYPE_STRING -> this.readLenEncString().toString(Charsets.UTF_8)
                else -> this.readLenEncString().toString(Charsets.UTF_8)
            }

        private fun ByteBuf.readDateTime(): LocalDateTime {
            val length = this.readFixedLengthInteger(1).toInt()
            when (length) {
                0 -> return LocalDateTime.MIN // Represents a NULL datetime
                4 -> {
                    val year = this.readFixedLengthInteger(2).toInt()
                    val month = this.readFixedLengthInteger(1).toInt()
                    val day = this.readFixedLengthInteger(1).toInt()
                    return LocalDateTime.of(year, month, day, 0, 0)
                }
                7 -> {
                    val year = this.readFixedLengthInteger(2).toInt()
                    val month = this.readFixedLengthInteger(1).toInt()
                    val day = this.readFixedLengthInteger(1).toInt()
                    val hour = this.readFixedLengthInteger(1).toInt()
                    val minute = this.readFixedLengthInteger(1).toInt()
                    return LocalDateTime.of(year, month, day, hour, minute)
                }
                11 -> {
                    val year = this.readFixedLengthInteger(2).toInt()
                    val month = this.readFixedLengthInteger(1).toInt()
                    val day = this.readFixedLengthInteger(1).toInt()
                    val hour = this.readFixedLengthInteger(1).toInt()
                    val minute = this.readFixedLengthInteger(1).toInt()
                    val second = this.readFixedLengthInteger(1).toInt()
                    val microseconds = this.readFixedLengthInteger(4).toInt() // Not used in LocalDateTime
                    return LocalDateTime.of(year, month, day, hour, minute, second, microseconds * 1000)
                }
                else -> throw IllegalArgumentException("Unsupported datetime length: $length, expected 0, 4, 7, or 11")
            }
        }

        private fun ByteBuf.readTime(): LocalDateTime {
            val length = this.readFixedLengthInteger(1).toInt()
            return when (length) {
                0 -> LocalDateTime.MIN // Represents a NULL time
                3 -> {
                    val hour = this.readFixedLengthInteger(1).toInt()
                    val minute = this.readFixedLengthInteger(1).toInt()
                    LocalDateTime.of(1970, 1, 1, hour, minute, 0) // Date is arbitrary
                }
                5 -> {
                    val hour = this.readFixedLengthInteger(1).toInt()
                    val minute = this.readFixedLengthInteger(1).toInt()
                    val second = this.readFixedLengthInteger(1).toInt()
                    LocalDateTime.of(1970, 1, 1, hour, minute, second) // Date is arbitrary
                }
                else -> throw IllegalArgumentException("Unsupported time length: $length, expected 0, 3, or 5")
            }
        }
    }

    private class BinaryProtocolResultsetState : MySqlGatewayState() {
        private var state: State = State.INITIAL
        private var columnCount: ULong = 0UL
        private val columnDefinitions = mutableListOf<ColumnDefinition41>()

        override fun onBackendMessage(
            ctx: ChannelHandlerContext,
            msg: Packet,
        ): StateResult =
            when (state) {
                State.INITIAL -> handleInitialState(ctx, msg)
                State.COLUMN_DEFINITION -> handleColumnDefinitionState(ctx, msg)
                State.RESULTSET_ROW -> handleResultsetRow(ctx, msg)
            }

        private fun handleInitialState(
            ctx: ChannelHandlerContext,
            packet: Packet,
        ): StateResult {
            val payload = packet.payload

            columnCount = payload.readLenEncInteger()
            logger.trace { "Column Count: $columnCount" }

            check(columnCount > 0UL) { "Column count must be greater than 0, but was $columnCount" }

            state = State.COLUMN_DEFINITION
            return StateResult(
                nextState = this,
                action = MessageAction.Forward,
            )
        }

        private fun handleColumnDefinitionState(
            ctx: ChannelHandlerContext,
            packet: Packet,
        ): StateResult {
            val columnDef = ColumnDefinition41.readFrom(packet.payload)
            logger.trace { "Received Column Definition: $columnDef" }

            columnDefinitions.add(columnDef)

            state =
                if (columnDefinitions.size == columnCount.toInt()) {
                    State.RESULTSET_ROW
                } else {
                    State.COLUMN_DEFINITION
                }

            return StateResult(
                nextState = this,
                action = MessageAction.Forward,
            )
        }

        private fun handleResultsetRow(
            ctx: ChannelHandlerContext,
            packet: Packet,
        ): StateResult {
            val payload = packet.payload

            // TODO: Handle OK Packet if CLIENT_DEPRECATE_EOF is set
            if (packet.isEofPacket()) {
                logger.trace {
                    val packet = Packet.Eof.readFrom(payload, ctx.capabilities().enumSet())
                    "Received Resultset EOF: $packet"
                }

                return StateResult(
                    nextState = CommandPhaseState(),
                    action = MessageAction.Forward,
                )
            }

            val packetHeader = payload.readFixedLengthInteger(1).toUInt()
            check(packetHeader == 0x00U) {
                "Expected column definition packet header 0x00, but received: 0x${"%02x".format(packetHeader).uppercase()}"
            }

            val offset = 2
            val nullBitmapLength = ((columnCount + 7U + offset.toULong()) / 8U).toInt()
            val nullBitmap = Bitmap.readFrom(payload, nullBitmapLength)
            val values =
                (0 until columnCount.toInt()).map { i ->
                    if (nullBitmap.get(i + offset)) {
                        null // Column value is NULL
                    } else {
                        val columnType = columnDefinitions[i].type
                        payload.readFieldValue(columnType)
                    }
                }

            logger.trace { "BinaryProtocolResultset row: $values" }

            return StateResult(
                nextState = this,
                action = MessageAction.Forward,
            )
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
