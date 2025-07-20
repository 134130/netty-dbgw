package com.github.l34130.netty.dbgw.app.handler.codec.mysql.command

import com.github.l34130.netty.dbgw.app.handler.codec.mysql.GatewayState
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.Packet
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.readFixedLengthInteger
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.upstream
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.channel.ChannelHandlerContext

// https://dev.mysql.com/doc/dev/mysql-server/latest/page_protocol_com_stmt_close.html
class CloseStatementCommandState : GatewayState {
    private var requested = false
    var statementId: ULong = 0UL

    override fun onDownstreamPacket(
        ctx: ChannelHandlerContext,
        packet: Packet,
    ): GatewayState {
        check(!requested) { "Duplicate COM_STMT_CLOSE request received." }
        requested = true

        val payload = packet.payload
        payload.markReaderIndex()

        val commandType = CommandPhaseState.CommandType.from(payload.readUnsignedByte().toUInt())
        check(commandType == CommandPhaseState.CommandType.COM_STMT_CLOSE) {
            "Expected COM_STMT_CLOSE command, but received: $commandType"
        }

        statementId = payload.readFixedLengthInteger(4)

        logger.trace { "COM_STMT_CLOSE: statementId=$statementId" }

        payload.resetReaderIndex()
        ctx.upstream().writeAndFlush(packet)
        return CommandPhaseState()
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}
