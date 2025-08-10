package com.github.l34130.netty.dbgw.protocol.mysql.command

import com.github.l34130.netty.dbgw.core.MessageAction
import com.github.l34130.netty.dbgw.core.util.netty.peek
import com.github.l34130.netty.dbgw.protocol.mysql.MySqlGatewayState
import com.github.l34130.netty.dbgw.protocol.mysql.Packet
import com.github.l34130.netty.dbgw.protocol.mysql.capabilities
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.channel.ChannelHandlerContext

internal class QuitCommandState : MySqlGatewayState() {
    private var requested = false

    override fun onFrontendMessage(
        ctx: ChannelHandlerContext,
        msg: Packet,
    ): StateResult {
        check(!requested) { "Duplicate COM_QUIT request received." }
        requested = true

        return StateResult(
            nextState = this,
            action = MessageAction.Forward,
        )
    }

    override fun onBackendMessage(
        ctx: ChannelHandlerContext,
        msg: Packet,
    ): StateResult {
        check(requested) { "Received COM_QUIT response without a prior request." }
        check(msg.isErrorPacket()) { "Expected an error packet for COM_QUIT, but got: $msg" }

        logger.trace {
            val errPacket = msg.payload.peek { Packet.Error.readFrom(it, ctx.capabilities().enumSet()) }
            "COM_QUIT response: $errPacket"
        }

        return StateResult(
            nextState = CommandPhaseState(),
            action = MessageAction.Terminate(reason = "COM_QUIT command executed"),
        )
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}
