package com.github.l34130.netty.dbgw.app.handler.codec.mysql.command

import com.github.l34130.netty.dbgw.app.handler.codec.mysql.GatewayState
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.Packet
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.capabilities
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.constant.CapabilityFlag
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.downstream
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.readFixedLengthInteger
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.readRestOfPacketString
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.upstream
import com.github.l34130.netty.dbgw.utils.netty.peek
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.channel.ChannelHandlerContext

// https://dev.mysql.com/doc/dev/mysql-server/latest/page_protocol_com_stmt_prepare.html
class CommandPrepareStatementState : GatewayState {
    private var requested = false

    override fun onDownstreamPacket(
        ctx: ChannelHandlerContext,
        packet: Packet,
    ): GatewayState {
        check(!requested) { "Duplicate COM_PREPARE request received." }
        requested = true

        logger.trace {
            val query = packet.payload.peek { it.readRestOfPacketString().toString(Charsets.UTF_8) }
            "Received COM_PREPARE request for query: $query"
        }

        ctx.upstream().writeAndFlush(packet)
        return this
    }

    override fun onUpstreamPacket(
        ctx: ChannelHandlerContext,
        packet: Packet,
    ): GatewayState {
        check(requested) { "COM_PREPARE response received without a prior request." }

        val payload = packet.payload
        payload.markReaderIndex()

        if (packet.isErrorPacket()) {
            logger.trace {
                val errPacket = payload.peek { Packet.Error.readFrom(it, ctx.capabilities().enumSet()) }
                "Received COM_STMT_PREPARE error response: $errPacket"
            }
            payload.resetReaderIndex()
            ctx.downstream().writeAndFlush(packet)
            return CommandPhaseState()
        }

        val firstByte = payload.peek { it.readUnsignedByte().toUInt() }
        if (firstByte != 0x00u) {
            error("Unexpected first byte during COM_STMT_PREPARE: 0x${firstByte?.toString(16)?.uppercase()}")
        }

        payload.skipBytes(1) // Skip the first byte (0x00)
        val statementId = payload.readFixedLengthInteger(4).toLong()
        val numColumns = payload.readFixedLengthInteger(2).toInt()
        val numParams = payload.readFixedLengthInteger(2).toInt()
        payload.skipBytes(1) // Skip the reserved byte

        var warningCount = 0
        var metadataFollows = false
        if (payload.readableBytes() > 0) {
            warningCount = payload.readFixedLengthInteger(2).toInt()
            if (ctx.capabilities().contains(CapabilityFlag.CLIENT_OPTIONAL_RESULTSET_METADATA)) {
                metadataFollows = payload.readFixedLengthInteger(1).toInt() == 1
            }
        }

        logger.trace {
            "COM_STMT_PREPARE response: statementId=$statementId, numColumns=$numColumns, numParams=$numParams, warningCount=$warningCount, metadataFollows=$metadataFollows"
        }

        ctx.downstream().writeAndFlush(packet)
        TODO("Not yet implemented: handle COM_STMT_PREPARE response with metadata and parameters")
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
