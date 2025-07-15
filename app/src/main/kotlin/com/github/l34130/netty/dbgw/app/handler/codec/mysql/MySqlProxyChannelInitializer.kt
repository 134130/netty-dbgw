package com.github.l34130.netty.dbgw.app.handler.codec.mysql

import com.github.l34130.netty.dbgw.app.handler.codec.mysql.connection.InitialHandshakeRequestHandler
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelInitializer

class MySqlProxyChannelInitializer(
    private val upstreamProvider: () -> Pair<String, Int>,
) : ChannelInitializer<Channel>() {
    override fun initChannel(ch: Channel) {
        val downstream = ch
        val proxyContext = ProxyContext(downstream)

        downstream
            .pipeline()
            .addLast(ProxyDownstreamHandler(proxyContext, upstreamProvider))
    }

    class ProxyDownstreamHandler(
        private val proxyContext: ProxyContext,
        private val upstreamProvider: () -> Pair<String, Int>,
    ) : ChannelInboundHandlerAdapter() {
        override fun channelActive(ctx: ChannelHandlerContext) {
            val (inetHost, inetPort) = this.upstreamProvider()
            proxyContext.connectUpstream(inetHost, inetPort)
            ctx
                .pipeline()
                .addLast("packet-encoder", PacketEncoder())
                .addLast("packet-decoder", PacketDecoder())
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
                .addLast("initial-handshake-request-handler", InitialHandshakeRequestHandler(proxyContext))
                .addLast("relay-handler", RelayHandler(proxyContext, proxyContext.downstream(), "upstream"))
        }
    }
}
