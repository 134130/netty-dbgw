package com.github.l34130.netty.dbgw.app.handler.codec.mysql

import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.socket.nio.NioSocketChannel

class ProxyContext(
    private val downstreamChannel: Channel,
) {
    private lateinit var upstreamChannelFuture: ChannelFuture
    private val upstreamBootstrap: Bootstrap = Bootstrap()

    lateinit var serverCapabilities: CapabilitiesFlags
    lateinit var clientCapabilities: CapabilitiesFlags

    fun downstream(): Channel = downstreamChannel

    fun connectUpstream(): ChannelFuture {
        if (::upstreamChannelFuture.isInitialized) {
            throw IllegalStateException("Upstream channel is already initialized.")
        }

        upstreamBootstrap
            .group(downstreamChannel.eventLoop()) // downstream과 동일한 EventLoopGroup 사용
            .channel(NioSocketChannel::class.java)
            .remoteAddress("mysql.querypie.io", 3307)
            .handler(MySqlProxyChannelInitializer.UpstreamInboundHandler(this))

        upstreamChannelFuture = upstreamBootstrap.connect()
        return upstreamChannelFuture
    }

    fun upstream(): Channel {
        if (!::upstreamChannelFuture.isInitialized) {
            throw IllegalStateException("Upstream channel is not initialized. Call connectUpstream() first.")
        }
        return upstreamChannelFuture.channel()
    }
}
