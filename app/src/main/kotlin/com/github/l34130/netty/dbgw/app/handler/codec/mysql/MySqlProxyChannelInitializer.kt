package com.github.l34130.netty.dbgw.app.handler.codec.mysql

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
            .addLast(DownstreamInboundHandler(proxyContext))
    }

    class DownstreamInboundHandler(
        private val proxyContext: ProxyContext,
    ) : ChannelInboundHandlerAdapter() {
        override fun channelActive(ctx: ChannelHandlerContext) {
            proxyContext.connectUpstream()
            ctx
                .pipeline()
                .addLast(PacketEncoder())
                .addLast(PacketDecoder())
//                .addLast("initial-handshake-handler", InitialHandshakeResponseInboundHandler(proxyContext))
                .addLast("relay-handler", RelayHandler(proxyContext.upstream(), "downstream"))
        }
    }

    class UpstreamInboundHandler(
        private val proxyContext: ProxyContext,
    ) : ChannelInboundHandlerAdapter() {
        override fun channelActive(ctx: ChannelHandlerContext) {
            ctx
                .pipeline()
                .addLast(PacketEncoder())
                .addLast(PacketDecoder())
//                .addLast("initial-handshake-handler", InitialHandshakeRequestInboundHandler(proxyContext))
                .addLast("relay-handler", RelayHandler(proxyContext.downstream(), "  upstream"))
        }
    }
}
