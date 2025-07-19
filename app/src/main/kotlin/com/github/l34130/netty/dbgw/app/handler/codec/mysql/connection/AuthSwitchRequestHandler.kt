package com.github.l34130.netty.dbgw.app.handler.codec.mysql.connection

import com.github.l34130.netty.dbgw.app.handler.codec.mysql.Packet
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.ProxyContext
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.command.DebugCommandHandler
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.command.PingCommandHandler
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.command.QueryCommandHandler
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.command.QuitCommandHandler
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.readNullTerminatedString
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler

class AuthSwitchRequestHandler(
    private val proxyContext: ProxyContext,
) : SimpleChannelInboundHandler<Packet>() {
    override fun channelRead0(
        ctx: ChannelHandlerContext,
        msg: Packet,
    ) {
        if (msg.isEofPacket()) {
            msg.payload.markReaderIndex()
            msg.payload.skipBytes(1) // Skip the first byte (EOF marker)

            logger.trace {
                val pluginName = msg.payload.readNullTerminatedString().toString(Charsets.US_ASCII)
                "Received AuthSwitchRequest with plugin: $pluginName"
            }

            msg.payload.resetReaderIndex()
            ctx.pipeline().remove(this)
            proxyContext.downstream().apply {
                pipeline().addBefore("relay-handler", "auth-switch-handler", AuthSwitchResponseHandler(proxyContext))
                writeAndFlush(msg)
            }
            return
        }

        if (msg.isOkPacket()) {
            // Authentication succeeded, no action needed
            logger.trace { "Authentication succeeded" }
            ctx.pipeline().remove(this)
            registerCommandPhaseHandlers()
            proxyContext.downstream().writeAndFlush(msg)
            return
        }

        if (msg.isErrorPacket()) {
            // Authentication failed, log the error
            logger.trace {
                msg.payload.markReaderIndex()
                "Authentication failed: ${Packet.Error.readFrom(msg.payload, proxyContext.capabilities())}"
                msg.payload.resetReaderIndex()
            }
            ctx.pipeline().remove(this)
            registerCommandPhaseHandlers()
            proxyContext.downstream().writeAndFlush(msg)
            return
        }

        // AuthMoreData packet will be here
        TODO("Not yet implemented: handle other types of packets during authentication")
    }

    private fun registerCommandPhaseHandlers() {
        proxyContext
            .downstream()
            .pipeline()
            .addBefore("relay-handler", "com-query-handler", QueryCommandHandler(proxyContext))
            .addAfter("com-query-handler", "com-debug-handler", DebugCommandHandler(proxyContext))
            .addAfter("com-debug-handler", "com-ping-handler", PingCommandHandler(proxyContext))
            .addAfter("com-ping-handler", "com-quit-handler", QuitCommandHandler(proxyContext))
        // TODO: com-set-option-handler, com-reset-connection-handler, com-change-user-handler
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}
