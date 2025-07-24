package com.github.l34130.netty.dbgw.core

import com.github.l34130.netty.dbgw.core.config.GatewayConfig
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelHandler
import io.netty.channel.MultiThreadIoEventLoopGroup
import io.netty.channel.nio.NioIoHandler
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler

@Deprecated("")
abstract class AbstractGateway(
    private val config: GatewayConfig,
) {
    private lateinit var f: ChannelFuture

    private val factory = NioIoHandler.newFactory()
    private val bossGroup = MultiThreadIoEventLoopGroup(factory)
    private val workerGroup = MultiThreadIoEventLoopGroup(factory)

    abstract fun createChannelInitializer(): ChannelHandler

    fun start() {
        val b =
            ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel::class.java)
                .handler(LoggingHandler(LogLevel.INFO))
                .childHandler(createChannelInitializer())

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
}
