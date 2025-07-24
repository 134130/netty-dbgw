package com.github.l34130.netty.dbgw.protocol.mysql.command

import com.github.l34130.netty.dbgw.core.MessageAction
import com.github.l34130.netty.dbgw.protocol.mysql.MySqlGatewayState
import com.github.l34130.netty.dbgw.protocol.mysql.Packet
import io.netty.channel.ChannelHandlerContext

internal class PingCommandState : MySqlGatewayState() {
    private var requested = false

    override fun onDownstreamMessage(
        ctx: ChannelHandlerContext,
        msg: Packet,
    ): StateResult {
        check(!requested) { "Duplicate COM_PING request received." }
        requested = true
        return StateResult(
            nextState = this,
            action = MessageAction.Forward,
        )
    }

    override fun onUpstreamMessage(
        ctx: ChannelHandlerContext,
        msg: Packet,
    ): StateResult {
        check(requested) { "Received COM_PING response without a prior request." }
        check(msg.isOkPacket()) { "Expected OK packet for COM_PING, but got: $msg" }

        return StateResult(
            nextState = CommandPhaseState(),
            action = MessageAction.Forward,
        )
    }
}
