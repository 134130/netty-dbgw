package com.github.l34130.netty.dbgw.protocol.mysql.command

import com.github.l34130.netty.dbgw.core.downstream
import com.github.l34130.netty.dbgw.core.upstream
import com.github.l34130.netty.dbgw.core.utils.netty.peek
import com.github.l34130.netty.dbgw.protocol.mysql.MySqlGatewayState
import com.github.l34130.netty.dbgw.protocol.mysql.Packet
import com.github.l34130.netty.dbgw.protocol.mysql.capabilities
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.channel.ChannelHandlerContext

internal class DebugCommandState : MySqlGatewayState {
    private var requested = false

    override fun onDownstreamMessage(
        ctx: ChannelHandlerContext,
        msg: Packet,
    ): MySqlGatewayState {
        check(!requested) { "Duplicate COM_DEBUG request received." }
        requested = true
        ctx.upstream().writeAndFlush(msg)
        return this
    }

    override fun onUpstreamMessage(
        ctx: ChannelHandlerContext,
        msg: Packet,
    ): MySqlGatewayState {
        check(requested) { "Received COM_DEBUG response without a prior request." }
        when {
            msg.isOkPacket() -> {
                logger.trace {
                    val okPacket = msg.payload.peek { Packet.Ok.readFrom(it, ctx.capabilities().enumSet()) }
                    "COM_DEBUG response: $okPacket"
                }
            }

            msg.isErrorPacket() -> {
                logger.trace {
                    val errorPacket = msg.payload.peek { Packet.Error.readFrom(it, ctx.capabilities().enumSet()) }
                    "COM_DEBUG response: $errorPacket"
                }
            }

            else -> logger.warn { "Unexpected COM_DEBUG response: $msg" }
        }

        ctx.downstream().writeAndFlush(msg)
        return CommandPhaseState()
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}
