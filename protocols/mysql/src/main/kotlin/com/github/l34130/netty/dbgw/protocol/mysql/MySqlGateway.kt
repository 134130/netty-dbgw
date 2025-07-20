package com.github.l34130.netty.dbgw.protocol.mysql

import com.github.l34130.netty.dbgw.config.GatewayConfig
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelFuture
import io.netty.channel.MultiThreadIoEventLoopGroup
import io.netty.channel.nio.NioIoHandler
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler

class MySqlGateway(
    private val config: GatewayConfig,
) {
    private lateinit var f: ChannelFuture

    private val factory = NioIoHandler.newFactory()
    private val bossGroup = MultiThreadIoEventLoopGroup(factory)
    private val workerGroup = MultiThreadIoEventLoopGroup(factory)

    fun start() {
        val b =
            ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel::class.java)
                .handler(LoggingHandler(LogLevel.INFO))
                .childHandler(MySqlProxyChannelInitializer(config))

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
