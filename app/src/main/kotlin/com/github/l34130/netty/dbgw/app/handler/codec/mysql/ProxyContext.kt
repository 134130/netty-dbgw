package com.github.l34130.netty.dbgw.app.handler.codec.mysql

import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture

class ProxyContext(
    private val downstreamChannel: Channel,
) {
    private lateinit var upstreamChannelFuture: ChannelFuture
    private val upstreamBootstrap: Bootstrap = Bootstrap()

    lateinit var serverCapabilities: Capabilities
    lateinit var clientCapabilities: Capabilities

    fun downstream(): Channel = downstreamChannel

    fun connectUpstream(): ChannelFuture {
        if (::upstreamChannelFuture.isInitialized) {
            throw IllegalStateException("Upstream channel is already initialized.")
        }

        upstreamBootstrap
            .group(downstreamChannel.eventLoop())
            .channel(downstreamChannel.javaClass)
            .handler(MySqlProxyChannelInitializer.ProxyUpstreamHandler(this))

        upstreamChannelFuture = upstreamBootstrap.connect("mysql.querypie.io", 3306)
        return upstreamChannelFuture
    }

    fun upstream(): Channel {
        if (!::upstreamChannelFuture.isInitialized) {
            throw IllegalStateException("Upstream channel is not initialized. Call connectUpstream() first.")
        }
        return upstreamChannelFuture.channel()
    }
}
