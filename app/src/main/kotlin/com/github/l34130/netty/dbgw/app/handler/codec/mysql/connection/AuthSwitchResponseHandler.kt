package com.github.l34130.netty.dbgw.app.handler.codec.mysql.connection

import com.github.l34130.netty.dbgw.app.handler.codec.mysql.Packet
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.ProxyContext
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler

class AuthSwitchResponseHandler(
    private val proxyContext: ProxyContext,
) : SimpleChannelInboundHandler<Packet>() {
    override fun channelRead0(
        ctx: ChannelHandlerContext,
        msg: Packet,
    ) {
        logger.trace {
            "Received AuthSwitchResponse: ${msg.payload.readableBytes()} bytes"
        }
        ctx.pipeline().remove(this)
        // TODO: Register next phase handlers
        proxyContext.upstream().writeAndFlush(msg)
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
