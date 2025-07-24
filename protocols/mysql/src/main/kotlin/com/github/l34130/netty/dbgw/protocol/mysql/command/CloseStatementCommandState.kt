package com.github.l34130.netty.dbgw.protocol.mysql.command

import com.github.l34130.netty.dbgw.core.MessageAction
import com.github.l34130.netty.dbgw.protocol.mysql.MySqlGatewayState
import com.github.l34130.netty.dbgw.protocol.mysql.Packet
import com.github.l34130.netty.dbgw.protocol.mysql.readFixedLengthInteger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.channel.ChannelHandlerContext

// https://dev.mysql.com/doc/dev/mysql-server/latest/page_protocol_com_stmt_close.html
internal class CloseStatementCommandState : MySqlGatewayState() {
    private var requested = false
    var statementId: ULong = 0UL

    override fun onDownstreamMessage(
        ctx: ChannelHandlerContext,
        msg: Packet,
    ): StateResult {
        check(!requested) { "Duplicate COM_STMT_CLOSE request received." }
        requested = true

        val payload = msg.payload
        val commandType = CommandPhaseState.CommandType.from(payload.readUnsignedByte().toUInt())
        check(commandType == CommandPhaseState.CommandType.COM_STMT_CLOSE) {
            "Expected COM_STMT_CLOSE command, but received: $commandType"
        }

        statementId = payload.readFixedLengthInteger(4)

        logger.trace { "COM_STMT_CLOSE: statementId=$statementId" }

        return StateResult(
            nextState = CommandPhaseState(),
            action = MessageAction.Forward,
        )
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}
