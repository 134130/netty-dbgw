package com.github.l34130.netty.dbgw.protocol.postgres.startup

import com.github.l34130.netty.dbgw.core.DatabaseGatewayState
import com.github.l34130.netty.dbgw.core.MessageAction
import com.github.l34130.netty.dbgw.protocol.postgres.Message
import com.github.l34130.netty.dbgw.protocol.postgres.command.QueryCycleStatus
import com.github.l34130.netty.dbgw.protocol.postgres.command.ReadyForQuery
import com.github.l34130.netty.dbgw.protocol.postgres.message.ParameterStatusMessage
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.channel.ChannelHandlerContext

class AuthenticationResultState : DatabaseGatewayState<Message, Message>() {
    override fun onBackendMessage(
        ctx: ChannelHandlerContext,
        msg: Message,
    ): StateResult =
        when (msg.type) {
            'R' -> {
                StateResult(
                    nextState = this,
                    action = MessageAction.Forward,
                )
            }
            'E' -> {
                TODO("Error handling not implemented yet")
            }
            'S' -> {
                val parameterStatusMessage = ParameterStatusMessage.readFrom(msg)
                logger.trace { "Parameter status: $parameterStatusMessage" }
                StateResult(
                    nextState = this,
                    action = MessageAction.Forward,
                )
            }
            'K' -> {
                val backendKeyData = BackendKeyData.readFrom(msg)
                logger.trace { "Backend key data: $backendKeyData" }
                StateResult(
                    nextState = this,
                    action = MessageAction.Forward,
                )
            }
            'Z' -> {
                val readyForQuery = ReadyForQuery.readFrom(msg)
                logger.trace { "Ready for query: $readyForQuery" }
                StateResult(
                    nextState = QueryCycleStatus(),
                    action = MessageAction.Forward,
                )
            }
            else -> error("Unexpected message type '${msg.type}' in AuthenticationResultState")
        }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
