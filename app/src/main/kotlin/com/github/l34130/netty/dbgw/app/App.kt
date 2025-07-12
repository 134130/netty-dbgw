package com.github.l34130.app.com.github.l34130.netty.dbgw.app

import com.github.l34130.netty.dbgw.app.handler.codec.mysql.MySqlProxyChannelInitializer
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.MultiThreadIoEventLoopGroup
import io.netty.channel.nio.NioIoHandler
import io.netty.channel.socket.nio.NioServerSocketChannel

fun main() {
    val factory = NioIoHandler.newFactory()
    val bossGroup = MultiThreadIoEventLoopGroup(factory)
    val workerGroup = MultiThreadIoEventLoopGroup(factory)

    try {
        val b =
            ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel::class.java)
                .childHandler(MySqlProxyChannelInitializer())

        val f = b.bind(8080).sync()
        f.channel().closeFuture().sync()
    } finally {
        workerGroup.shutdownGracefully()
        bossGroup.shutdownGracefully()
    }
}
