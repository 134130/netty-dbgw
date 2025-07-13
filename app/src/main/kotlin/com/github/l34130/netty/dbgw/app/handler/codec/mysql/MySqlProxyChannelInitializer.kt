package com.github.l34130.netty.dbgw.app.handler.codec.mysql

import com.github.l34130.netty.dbgw.app.handler.codec.mysql.connection.InitialHandshakeRequestHandler
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.connection.InitialHandshakeResponseHandler
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelInitializer

class MySqlProxyChannelInitializer : ChannelInitializer<Channel>() {
    override fun initChannel(ch: Channel) {
        val downstream = ch
        val proxyContext = ProxyContext(downstream)

        downstream
            .pipeline()
            .addLast(ProxyDownstreamHandler(proxyContext))
    }

    class ProxyDownstreamHandler(
        private val proxyContext: ProxyContext,
    ) : ChannelInboundHandlerAdapter() {
        override fun channelActive(ctx: ChannelHandlerContext) {
            proxyContext.connectUpstream()
            ctx
                .pipeline()
                .addLast("packet-encoder", PacketEncoder())
                .addLast("packet-decoder", PacketDecoder())
                .addLast("initial-handshake-handler", InitialHandshakeResponseHandler(proxyContext))
                .addLast("relay-handler", RelayHandler(proxyContext, proxyContext.upstream(), "downstream"))
        }
    }

    class ProxyUpstreamHandler(
        private val proxyContext: ProxyContext,
    ) : ChannelInboundHandlerAdapter() {
        override fun channelActive(ctx: ChannelHandlerContext) {
            ctx
                .pipeline()
                .addLast("packet-encoder", PacketEncoder())
                .addLast("packet-decoder", PacketDecoder())
                .addLast("initial-handshake-handler", InitialHandshakeRequestHandler(proxyContext))
                .addLast("relay-handler", RelayHandler(proxyContext, proxyContext.downstream(), "upstream"))
        }
    }
}
