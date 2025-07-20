package com.github.l34130.netty.dbgw.app.handler.codec.mysql.command

import com.github.l34130.netty.dbgw.app.handler.codec.mysql.GatewayState
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.Packet
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.capabilities
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.constant.CapabilityFlag
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.constant.MySqlFieldType
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.readFixedLengthInteger
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.readLenEncInteger
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.readLenEncString
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.readRestOfPacketString
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.upstream
import com.github.l34130.netty.dbgw.utils.netty.peek
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.channel.ChannelHandlerContext

class CommandPhaseState : GatewayState {
    override fun onDownstreamPacket(
        ctx: ChannelHandlerContext,
        packet: Packet,
    ): GatewayState {
        val payload = packet.payload
        val commandByte = payload.peek { it.readUnsignedByte().toUInt() } ?: error("Command byte is missing in the packet")
        val commandType = CommandType.from(commandByte)
        logger.debug { "Received $commandType" }
        return when (commandType) {
            CommandType.COM_QUERY -> handleQueryCommand(ctx, packet)
            CommandType.COM_PING -> PingCommandState().onDownstreamPacket(ctx, packet)
            CommandType.COM_QUIT -> QuitCommandState().onDownstreamPacket(ctx, packet)
            CommandType.COM_DEBUG -> DebugCommandState().onDownstreamPacket(ctx, packet)
            CommandType.COM_STMT_PREPARE -> PrepareStatementCommandState().onDownstreamPacket(ctx, packet)
            CommandType.COM_STMT_EXECUTE -> ExecuteStatementCommandState().onDownstreamPacket(ctx, packet)
            CommandType.COM_STMT_CLOSE -> CloseStatementCommandState().onDownstreamPacket(ctx, packet)
            null -> throw IllegalArgumentException("Unknown command byte: 0x${commandByte.toString(16).uppercase()}")
            else -> TODO("Unhandled command type: $commandType")
        }
    }

    private fun handleQueryCommand(
        ctx: ChannelHandlerContext,
        packet: Packet,
    ): GatewayState {
        val payload = packet.payload
        payload.markReaderIndex()
        payload.skipBytes(1) // skip command byte
        logger.trace { "Received COM_QUERY" }

        if (ctx.capabilities().contains(CapabilityFlag.CLIENT_QUERY_ATTRIBUTES)) {
            val parameterCount = payload.readLenEncInteger().toInt()
            val parameterSetCount = payload.readLenEncInteger().toInt() // always 1 currently
            logger.trace { "Parameter count: $parameterCount, Parameter set count: $parameterSetCount" }
            if (parameterCount > 0) {
                val nullBitmap = payload.readString((parameterCount + 7) / 8, Charsets.UTF_8)
                val nextParamsBindFlag = payload.readFixedLengthInteger(1)
                if (nextParamsBindFlag != 1UL) {
                    // malformed packet, unexpected nextParamsBindFlag
                    logger.warn { "Unexpected nextParamsBindFlag: $nextParamsBindFlag. Malformed packet" }
                }
                val parameters = mutableListOf<Triple<MySqlFieldType, String, Any?>>()
                (0 until parameterCount).forEach { i ->
                    val parameterTypeAndFlag = payload.readFixedLengthInteger(2)
                    val type = MySqlFieldType.of(parameterTypeAndFlag.toInt())
                    val parameterName = payload.readLenEncString()
                    parameters.add(Triple(type, parameterName.toString(Charsets.UTF_8), null))
                }
                logger.trace { "Parameters: $parameters" }
            }
        }

        val query = payload.readRestOfPacketString()
        logger.debug { "Query: '${query.toString(Charsets.UTF_8)}'" }

        payload.resetReaderIndex()
        ctx.upstream().writeAndFlush(packet)
        return QueryCommandResponseState()
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    enum class CommandType(
        val code: UInt,
    ) {
        COM_QUERY(0x03u),
        COM_PING(0x0Eu),
        COM_QUIT(0x01u),
        COM_DEBUG(0x0Du),
        COM_STMT_PREPARE(0x16u),
        COM_STMT_EXECUTE(0x17u),
        COM_STMT_CLOSE(0x19u),
        ;

        companion object {
            private val map = entries.associateBy(CommandType::code)

            fun from(code: UInt): CommandType? = map[code]
        }
    }
}
