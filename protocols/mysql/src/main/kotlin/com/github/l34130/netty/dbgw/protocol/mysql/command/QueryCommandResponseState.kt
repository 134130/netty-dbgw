package com.github.l34130.netty.dbgw.protocol.mysql.command

import com.github.l34130.netty.dbgw.protocol.mysql.GatewayState
import com.github.l34130.netty.dbgw.protocol.mysql.Packet
import com.github.l34130.netty.dbgw.protocol.mysql.capabilities
import com.github.l34130.netty.dbgw.protocol.mysql.constant.CapabilityFlag
import com.github.l34130.netty.dbgw.protocol.mysql.constant.ServerStatusFlag
import com.github.l34130.netty.dbgw.protocol.mysql.downstream
import com.github.l34130.netty.dbgw.protocol.mysql.readFixedLengthInteger
import com.github.l34130.netty.dbgw.protocol.mysql.readLenEncInteger
import com.github.l34130.netty.dbgw.protocol.mysql.readLenEncString
import com.github.l34130.netty.dbgw.utils.netty.peek
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext

class QueryCommandResponseState : GatewayState {
    override fun onUpstreamPacket(
        ctx: ChannelHandlerContext,
        packet: Packet,
    ): GatewayState {
        val payload = packet.payload
        payload.markReaderIndex()

        if (packet.isOkPacket()) {
            logger.trace {
                val okPacket = Packet.Ok.readFrom(payload, ctx.capabilities().enumSet())
                "Received COM_QUERY_RESPONSE: $okPacket"
            }
            packet.payload.resetReaderIndex()
            ctx.downstream().writeAndFlush(packet)
            return CommandPhaseState()
        }

        if (packet.isErrorPacket()) {
            logger.trace {
                val errorPacket = Packet.Error.readFrom(payload, ctx.capabilities().enumSet())
                "Received COM_QUERY_RESPONSE: $errorPacket"
            }
            packet.payload.resetReaderIndex()
            ctx.downstream().writeAndFlush(packet)
            return CommandPhaseState()
        }

        if (packet.payload.peek { it.readUnsignedByte().toUInt() } == 0xFBu) {
            TODO("Unhandled 0xFB byte in COM_QUERY_RESPONSE, this indicates a More Data packet.")
        }

        packet.payload.resetReaderIndex()
        return TextResultsetState().onUpstreamPacket(ctx, packet)
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    private class TextResultsetState : GatewayState {
        private var state: State = State.FIELD_COUNT
        private var columnCount: ULong = 0UL
        private var metadataFollows: Boolean = false
        private var columnDefinitionCount: ULong = 0UL

        override fun onUpstreamPacket(
            ctx: ChannelHandlerContext,
            packet: Packet,
        ): GatewayState {
            logger.trace { "Processing TextResultSet: currentState='${state.name}'" }

            val nextState =
                when (state) {
                    State.FIELD_COUNT -> handleFieldCountState(ctx, packet)
                    State.FIELD -> handleFieldState(ctx, packet)
                    State.EOF -> handleEofState(ctx, packet)
                    State.ROW -> handleRowState(ctx, packet)
                }

            logger.trace { "Transitioning to state: ${state.name}" }
            return nextState
        }

        private fun handleFieldCountState(
            ctx: ChannelHandlerContext,
            packet: Packet,
        ): GatewayState {
            val payload = packet.payload
            payload.markReaderIndex()

            if (ctx.capabilities().contains(CapabilityFlag.CLIENT_OPTIONAL_RESULTSET_METADATA)) {
                metadataFollows = payload.readFixedLengthInteger(1) == 1UL
            }
            this.columnCount = payload.readLenEncInteger()
            logger.trace { "Received column count: $columnCount, metadataFollows: $metadataFollows" }

            payload.resetReaderIndex()
            ctx.downstream().writeAndFlush(packet)

            this.state = State.FIELD
            return this
        }

        private fun handleFieldState(
            ctx: ChannelHandlerContext,
            packet: Packet,
        ): GatewayState {
            val payload = packet.payload
            payload.markReaderIndex()

            // https://dev.mysql.com/doc/dev/mysql-server/latest/page_protocol_com_query_response_text_resultset_column_definition.html
            logger.trace {
                val columnDefinition = ColumnDefinition41.readFrom(payload)
                columnDefinition.toString()
            }

            state =
                if (++columnDefinitionCount < columnCount) {
                    State.FIELD // Continue to next column definition
                } else {
                    State.EOF // All column definitions processed, move to EOF state
                }

            if (state == State.EOF && !ctx.capabilities().contains(CapabilityFlag.CLIENT_OPTIONAL_RESULTSET_METADATA)) {
                this.state = State.ROW
            }

            payload.resetReaderIndex()
            ctx.downstream().writeAndFlush(packet)
            return this
        }

        private fun handleEofState(
            ctx: ChannelHandlerContext,
            packet: Packet,
        ): GatewayState {
            logger.trace {
                val eofPacket =
                    packet.payload.peek {
                        Packet.Eof.readFrom(it, ctx.capabilities().enumSet())
                    }
                "End of metadata reached: $eofPacket"
            }
            state = State.ROW
            ctx.downstream().writeAndFlush(packet)
            return this
        }

        private fun handleRowState(
            ctx: ChannelHandlerContext,
            packet: Packet,
        ): GatewayState {
            val payload = packet.payload
            payload.markReaderIndex()

            handleTerminator(ctx, packet)?.let { nextState ->
                return nextState // If terminator was handled, return the next state
            }

            logger.trace {
                val rowData = payload.readTextResultsetRow(columnCount)
                "Row Data: $rowData"
            }

            payload.resetReaderIndex()
            ctx.downstream().writeAndFlush(packet)
            return this // Continue in the same state for more rows
        }

        private fun handleTerminator(
            ctx: ChannelHandlerContext,
            packet: Packet,
        ): GatewayState? {
            val payload = packet.payload
            payload.markReaderIndex()

            val capabilities = ctx.capabilities()

            if (packet.isErrorPacket()) {
                val errPacket = Packet.Error.readFrom(payload, capabilities.enumSet())
                logger.debug { "Text Resultset terminated: $errPacket" }

                payload.resetReaderIndex()
                ctx.downstream().writeAndFlush(errPacket)
                return CommandPhaseState() // Return to command phase
            }

            val moreResultsExists =
                if (capabilities.contains(CapabilityFlag.CLIENT_DEPRECATE_EOF) && packet.isOkPacket()) {
                    val packet = Packet.Ok.readFrom(payload, capabilities.enumSet())
                    logger.debug { "Text Resultset terminated: $packet" }
                    packet.statusFlags?.contains(ServerStatusFlag.SERVER_MORE_RESULTS_EXISTS) == true
                } else if (packet.isEofPacket()) {
                    val packet = Packet.Eof.readFrom(payload, capabilities.enumSet())
                    logger.debug { "Text Resultset terminated: $packet" }
                    packet.statusFlags?.contains(ServerStatusFlag.SERVER_MORE_RESULTS_EXISTS) == true
                } else {
                    return null // Not a terminator packet
                }

            val nextState =
                if (moreResultsExists) {
                    logger.debug { "More results exist, continuing to new Text Resultset" }
                    QueryCommandResponseState()
                } else {
                    CommandPhaseState() // No more results, return to command phase
                }

            payload.resetReaderIndex()
            ctx.downstream().writeAndFlush(packet)
            return nextState
        }

        // https://dev.mysql.com/doc/dev/mysql-server/latest/page_protocol_com_query_response_text_resultset_row.html
        private fun ByteBuf.readTextResultsetRow(columnCount: ULong): List<String?> {
            val result = mutableListOf<String?>()

            for (i in 0UL until columnCount) {
                if (this.readableBytes() == 0) {
                    logger.warn { "No more data to read for column $i (expected $columnCount columns)" }
                    return result
                }

                if (this.peek { it.readUnsignedByte() }?.toInt() == 0xFB) {
                    this.skipBytes(1) // 0xFB indicates the end of the row
                    // NULL
                    result.add(null)
                } else {
                    val value = this.readLenEncString()
                    result.add(value.toString(Charsets.UTF_8))
                }
            }

            return result
        }

        companion object {
            private val logger = KotlinLogging.logger { }
        }

        private enum class State {
            FIELD_COUNT,
            FIELD,
            EOF,
            ROW,
        }
    }
}
