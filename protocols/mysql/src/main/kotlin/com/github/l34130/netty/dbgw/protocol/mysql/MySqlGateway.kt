package com.github.l34130.netty.dbgw.protocol.mysql

import com.github.l34130.netty.dbgw.core.AbstractGatewayChannelInitializer
import com.github.l34130.netty.dbgw.core.GatewayStateMachine
import com.github.l34130.netty.dbgw.core.config.GatewayConfig
import com.github.l34130.netty.dbgw.core.security.QueryPolicy
import com.github.l34130.netty.dbgw.core.security.QueryPolicyEngine
import com.github.l34130.netty.dbgw.core.security.QueryPolicyResult
import com.github.l34130.netty.dbgw.protocol.mysql.command.PreparedStatement
import com.github.l34130.netty.dbgw.protocol.mysql.connection.HandshakeState
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelHandler
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
                .childHandler(MySqlGatewayChannelInitializer(config))

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

    private class MySqlGatewayChannelInitializer(
        config: GatewayConfig,
    ) : AbstractGatewayChannelInitializer<Packet, MySqlGatewayStateMachine>(config) {
        private val capabilities = Capabilities()
        private val preparedStatements: MutableMap<UInt, PreparedStatement> = mutableMapOf()
        private val queryPolicyEngine =
            config.restrictedSqlStatements
                .map { sql ->
                    QueryPolicy { query: String ->
                        if (query.contains(sql, ignoreCase = true)) {
                            QueryPolicyResult(
                                isAllowed = false,
                                reason = "Restricted SQL statement: $sql",
                            )
                        } else {
                            QueryPolicyResult(isAllowed = true)
                        }
                    }
                }.let {
                    QueryPolicyEngine(it)
                }

        override fun createStateMachine(): MySqlGatewayStateMachine = MySqlGatewayStateMachine()

        override fun createMessageDecoder(): ChannelHandler = PacketDecoder()

        override fun createMessageEncoder(): ChannelHandler = PacketEncoder()

        override fun downstreamChannelActive(channel: Channel) {
            channel.attr(MySqlAttrs.CAPABILITIES_ATTR_KEY).set(capabilities)
            channel.attr(MySqlAttrs.PREPARED_STATEMENTS_ATTR_KEY).set(preparedStatements)
            channel.attr(MySqlAttrs.QUERY_POLICY_ENGINE_ATTR_KEY).set(queryPolicyEngine)
        }

        override fun upstreamChannelActive(channel: Channel) {
            channel.attr(MySqlAttrs.CAPABILITIES_ATTR_KEY).set(capabilities)
            channel.attr(MySqlAttrs.PREPARED_STATEMENTS_ATTR_KEY).set(preparedStatements)
            channel.attr(MySqlAttrs.QUERY_POLICY_ENGINE_ATTR_KEY).set(queryPolicyEngine)
        }
    }

    private class MySqlGatewayStateMachine(
        initialState: MySqlGatewayState = HandshakeState(),
    ) : GatewayStateMachine<Packet>(initialState)
}
