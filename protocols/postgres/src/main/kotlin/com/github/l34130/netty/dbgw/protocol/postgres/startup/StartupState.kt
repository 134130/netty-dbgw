package com.github.l34130.netty.dbgw.protocol.postgres.startup

import com.github.l34130.netty.dbgw.core.DatabaseGatewayState
import com.github.l34130.netty.dbgw.core.MessageAction
import com.github.l34130.netty.dbgw.core.backend
import com.github.l34130.netty.dbgw.protocol.postgres.Message
import com.github.l34130.netty.dbgw.protocol.postgres.MessageDecoder
import com.github.l34130.netty.dbgw.protocol.postgres.MessageEncoder
import com.github.l34130.netty.dbgw.protocol.postgres.message.StartupMessage
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext

class StartupState : DatabaseGatewayState<ByteBuf, Message>() {
    override fun onFrontendMessage(
        ctx: ChannelHandlerContext,
        msg: ByteBuf,
    ): StateResult {
        msg.markReaderIndex()
        val startupMsg = StartupMessage.readFrom(msg)
        logger.debug { "$startupMsg" }

        // Need to reset the reader index because this does not reset the reader index by MessageDecoder
        msg.resetReaderIndex()

        ctx.pipeline().also {
            it.addFirst("encoder", MessageEncoder())
            it.addFirst("decoder", MessageDecoder())
        }
        ctx.backend().pipeline().also {
            it.addFirst("decoder", MessageDecoder())
            // encoder will be added later to escape the startup message encoding; StartupMessage is not a message format
        }

        return StateResult(
            nextState = AuthenticationState(),
            action = MessageAction.Forward,
        )
    }

    private val logger = KotlinLogging.logger { }
}
