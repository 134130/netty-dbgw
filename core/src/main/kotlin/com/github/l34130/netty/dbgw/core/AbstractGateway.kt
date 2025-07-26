package com.github.l34130.netty.dbgw.core

import com.github.l34130.netty.dbgw.core.config.DatabaseGatewayConfig
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelInitializer
import io.netty.channel.MultiThreadIoEventLoopGroup
import io.netty.channel.nio.NioIoHandler
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler

abstract class AbstractGateway(
    protected val config: DatabaseGatewayConfig,
) {
    protected abstract fun createFrontendHandlers(): List<ChannelHandler>

    protected abstract fun createBackendHandlers(): List<ChannelHandler>

    protected open fun createStateMachine(): StateMachine? = null

    private lateinit var f: ChannelFuture

    private val factory = NioIoHandler.newFactory()
    private val bossGroup = MultiThreadIoEventLoopGroup(1, factory)
    private val workerGroup = MultiThreadIoEventLoopGroup(factory)

    fun start() {
        val b =
            ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel::class.java)
                .handler(LoggingHandler(LogLevel.INFO))
                .childHandler(DatabaseGatewayChannelInitializer())

        f = b.bind(config.listenPort).sync()
    }

    fun stop() {
        try {
            if (::f.isInitialized && f.isSuccess) {
                f.channel().close().sync()
            } else {
                throw IllegalStateException("Gateway is not running or has not been started.")
            }
        } finally {
            workerGroup.shutdownGracefully()
            bossGroup.shutdownGracefully()
        }
    }

    fun shutdown() {
        try {
            if (::f.isInitialized && f.isSuccess) {
                f.channel().closeFuture().sync()
            } else {
                throw IllegalStateException("Gateway is not running or has not been started.")
            }
        } finally {
            workerGroup.shutdownGracefully()
            bossGroup.shutdownGracefully()
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
