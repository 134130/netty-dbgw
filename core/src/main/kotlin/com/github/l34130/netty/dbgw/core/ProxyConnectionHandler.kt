package com.github.l34130.netty.dbgw.core

import com.github.l34130.netty.dbgw.core.config.DatabaseGatewayConfig
import com.github.l34130.netty.dbgw.core.utils.netty.closeOnFlush
import com.github.l34130.netty.dbgw.policy.api.ClientInfo
import com.github.l34130.netty.dbgw.policy.api.SessionInfo
import com.github.l34130.netty.dbgw.policy.api.database.DatabaseConnectionInfo
import com.github.l34130.netty.dbgw.policy.api.database.DatabaseContext
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelInitializer
import io.netty.handler.timeout.IdleState
import io.netty.handler.timeout.IdleStateEvent
import io.netty.handler.timeout.IdleStateHandler
import java.net.InetSocketAddress
import java.util.UUID
import java.util.concurrent.TimeUnit

class ProxyConnectionHandler(
    private val config: DatabaseGatewayConfig,
    private val frontendHandlers: List<ChannelHandler>,
    private val backendHandlers: List<ChannelHandler>,
    private val stateMachine: StateMachine?,
) : ChannelInboundHandlerAdapter() {
    override fun channelActive(ctx: ChannelHandlerContext) {
        val frontend = ctx.channel()

        logger.debug { "Connected from frontend: ${frontend.remoteAddress()}" }

        val backendFuture =
            Bootstrap()
                .group(frontend.eventLoop())
                .channel(frontend.javaClass)
                .handler(
                    object : ChannelInitializer<Channel>() {
                        override fun initChannel(ch: Channel) {
                            ch.pipeline().addLast(
                                *backendHandlers.toTypedArray(),
                            )
                            stateMachine?.let {
                                ch.pipeline().addLast(
                                    "state-machine-handler",
                                    StateMachineHandler(it, MessageDirection.BACKEND),
                                )
                            }
                        }
                    },
                ).connect(config.upstreamHost, config.upstreamPort)

        val backend = backendFuture.channel()
        backendFuture.addListener { future ->
            if (!future.isSuccess) {
                logger.error(future.cause()) { "Failed to connect to backend: ${config.upstreamHost}:${config.upstreamPort}" }
                frontend.closeOnFlush()
                return@addListener
            }

            logger.debug { "Connected to backend: ${backend.remoteAddress()}" }

            frontend.attr(GatewayAttrs.BACKEND_ATTR_KEY).set(backend)
            backend.attr(GatewayAttrs.FRONTEND_ATTR_KEY).set(frontend)

            frontend.config().isAutoRead = true
            frontend.read()
        }

        frontend.pipeline().addLast(
            *frontendHandlers.toTypedArray(),
        )
        stateMachine?.let {
            frontend.pipeline().addLast(
                "state-machine-handler",
                StateMachineHandler(it, MessageDirection.FRONTEND),
            )
        }

        config.policyEngine?.let { policyEngine ->
            policyEngine.policies.find { it::class.simpleName == "DatabaseIdleTimeoutPolicy" }?.let { policy ->
                val timeoutSeconds = policy::class.members.first { it.name == "timeoutSeconds" }.call(policy) as Int
                frontend.pipeline().addFirst(
                    "idle-state-handler",
                    IdleStateHandler(0, 0, timeoutSeconds.toLong(), TimeUnit.SECONDS)
                )
                frontend.pipeline().addAfter(
                    "idle-state-handler",
                    "idle-timeout-handler",
                    IdleTimeoutHandler()
                )
            }
        }

        // Set the common gateway attributes for both frontend and backend channels
        val sessionInfo =
            SessionInfo(sessionId = UUID.randomUUID().toString()).also {
                frontend.attr(GatewayAttrs.SESSION_INFO_ATTR_KEY).set(it)
                backend.attr(GatewayAttrs.SESSION_INFO_ATTR_KEY).set(it)
            }
        val remoteAddress = frontend.remoteAddress()
        val sourceIp =
            when (remoteAddress) {
                is InetSocketAddress -> remoteAddress.address.hostAddress
                else -> remoteAddress.toString()
            }
        val clientInfo =
            ClientInfo(sourceIps = listOf(sourceIp)).also {
                frontend.attr(GatewayAttrs.CLIENT_INFO_ATTR_KEY).set(it)
                backend.attr(GatewayAttrs.CLIENT_INFO_ATTR_KEY).set(it)
            }

        // TODO: Only for Database Connection Handler
        // Set the database connection info attribute for both frontend and backend channels
        frontend.attr(GatewayAttrs.DATABASE_GATEWAY_CONFIG_ATTR_KEY).set(config)
        backend.attr(GatewayAttrs.DATABASE_GATEWAY_CONFIG_ATTR_KEY).set(config)

        val databaseConnectionInfo =
            DatabaseConnectionInfo(
                databaseType = config.upstreamDatabaseType.toString(),
            ).also {
                frontend.attr(GatewayAttrs.DATABASE_CONNECTION_INFO_ATTR_KEY).set(it)
                backend.attr(GatewayAttrs.DATABASE_CONNECTION_INFO_ATTR_KEY).set(it)
            }
        val databaseContext =
            DatabaseContext(
                clientInfo = clientInfo,
                connectionInfo = databaseConnectionInfo,
                sessionInfo = sessionInfo,
            ).also { ctx ->
                frontend.attr(GatewayAttrs.DATABASE_CONTEXT_ATTR_KEY).set(ctx)
                backend.attr(GatewayAttrs.DATABASE_CONTEXT_ATTR_KEY).set(ctx)
            }

        frontend.pipeline().remove(this)
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}
