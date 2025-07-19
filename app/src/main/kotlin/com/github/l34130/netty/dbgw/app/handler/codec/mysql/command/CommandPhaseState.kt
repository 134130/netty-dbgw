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
        val commandByte = payload.peek { it.readUnsignedByte().toUInt() }
        return when (commandByte) {
            COM_QUERY -> handleQueryCommand(ctx, packet)
            COM_PING -> handlePingCommand(ctx, packet)
            COM_QUIT -> handleQuitCommand(ctx, packet)
            else -> TODO("Unhandled command byte: 0x${commandByte?.toString(16)?.uppercase()}")
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
                for (i in 0 until parameterCount) {
                    val parameterTypeAndFlag = payload.readFixedLengthInteger(2)
                    val type = MySqlFieldType.of(parameterTypeAndFlag.toInt())
                    val parameterName = payload.readLenEncString()
                    parameters.add(Triple(type, parameterName.toString(Charsets.UTF_8), null))
                }
                logger.trace { "Parameters: $parameters" }
            }
        }

        val query = payload.readRestOfPacketString()
        logger.debug { "Query: ${query.toString(Charsets.UTF_8)}" }

        payload.resetReaderIndex()
        ctx.upstream().writeAndFlush(packet)
        return QueryCommandResponseState()
    }

    private fun handlePingCommand(
        ctx: ChannelHandlerContext,
        packet: Packet,
    ): GatewayState {
        logger.trace { "Received COM_PING" }
        return PingCommandState().onDownstreamPacket(ctx, packet)
    }

    private fun handleQuitCommand(
        ctx: ChannelHandlerContext,
        packet: Packet,
    ): GatewayState {
        logger.trace { "Received COM_QUIT" }
        return QuitCommandState().onDownstreamPacket(ctx, packet)
    }

    companion object {
        private val logger = KotlinLogging.logger {}
        private val COM_QUERY = 0x03u
        private val COM_PING = 0x0Eu
        private val COM_QUIT = 0x01u
    }
}
