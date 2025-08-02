package com.github.l34130.netty.dbgw.core

import com.github.l34130.netty.dbgw.core.config.DatabaseGatewayConfig
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelInitializer
import io.netty.channel.IoEventLoopGroup
import io.netty.channel.MultiThreadIoEventLoopGroup
import io.netty.channel.nio.NioIoHandler
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import java.net.InetSocketAddress

abstract class AbstractGateway(
    protected val config: DatabaseGatewayConfig,
) {
    private var bossGroup: IoEventLoopGroup? = null
    private var workerGroup: IoEventLoopGroup? = null

    private var channel: Channel? = null

    protected abstract fun createFrontendHandlers(): List<ChannelHandler>

    protected abstract fun createBackendHandlers(): List<ChannelHandler>

    protected open fun createStateMachine(): StateMachine? = null

    fun port(): Int = (channel!!.localAddress() as InetSocketAddress).port

    fun start() {
        bossGroup = MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory())
        workerGroup = MultiThreadIoEventLoopGroup(NioIoHandler.newFactory())
        val b =
            ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel::class.java)
                .handler(LoggingHandler(LogLevel.INFO))
                .childHandler(DatabaseGatewayChannelInitializer())

        val future = b.bind(config.listenPort).sync()
        channel = future.channel()
    }

    fun shutdown() {
        try {
            channel?.close()?.sync()
        } finally {
            workerGroup?.shutdownGracefully()
            bossGroup?.shutdownGracefully()
        }
    }

    private inner class DatabaseGatewayChannelInitializer : ChannelInitializer<Channel>() {
        override fun initChannel(ch: Channel) {
            val frontend = ch

            frontend.config().isAutoRead = false

            frontend
                .pipeline()
                .addLast(
                    ProxyConnectionHandler(
                        config = config,
                        frontendHandlers = createFrontendHandlers(),
                        backendHandlers = createBackendHandlers(),
                        stateMachine = createStateMachine(),
                    ),
                )
        }
    }
}
