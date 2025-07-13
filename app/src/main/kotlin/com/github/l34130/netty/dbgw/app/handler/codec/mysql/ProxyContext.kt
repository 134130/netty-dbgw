package com.github.l34130.netty.dbgw.app.handler.codec.mysql

import com.github.l34130.netty.dbgw.app.handler.codec.mysql.constant.CapabilityFlag
import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import java.util.EnumSet

class ProxyContext(
    private val downstreamChannel: Channel,
) {
    private lateinit var upstreamChannelFuture: ChannelFuture
    private val upstreamBootstrap: Bootstrap = Bootstrap()

    private var serverCapabilities: EnumSet<CapabilityFlag>? = null
    private var clientCapabilities: EnumSet<CapabilityFlag>? = null
    private val interCapabilities: EnumSet<CapabilityFlag> by lazy {
        EnumSet.copyOf(serverCapabilities!!.intersect(clientCapabilities!!))
    }

    fun downstream(): Channel = downstreamChannel

    fun upstream(): Channel {
        if (!::upstreamChannelFuture.isInitialized) {
            throw IllegalStateException("Upstream channel is not initialized. Call connectUpstream() first.")
        }
        return upstreamChannelFuture.channel()
    }

    fun capabilities(): EnumSet<CapabilityFlag> = interCapabilities

    fun setServerCapabilities(capabilities: EnumSet<CapabilityFlag>) {
        serverCapabilities = capabilities
    }

    fun setClientCapabilities(capabilities: EnumSet<CapabilityFlag>) {
        clientCapabilities = capabilities
    }

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
}
