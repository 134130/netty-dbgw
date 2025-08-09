package com.github.l34130.netty.dbgw.core

import com.github.l34130.netty.dbgw.core.utils.netty.closeOnFlush
import com.github.l34130.netty.dbgw.policy.api.ClientInfo
import com.github.l34130.netty.dbgw.policy.api.SessionInfo
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelInitializer
import java.net.InetSocketAddress
import java.util.UUID

class ProxyConnectionHandler(
    private val upstreamHost: String,
    private val upstreamPort: Int,
    private val frontendHandlers: List<ChannelHandler>,
    private val backendHandlers: List<ChannelHandler>,
    private val stateMachine: StateMachine?,
    private val onConnectionEstablished: (Channel, Channel) -> Unit = { _, _ -> },
) : ChannelInboundHandlerAdapter() {
    override fun channelActive(ctx: ChannelHandlerContext) {
        val frontend = ctx.channel()
        logger.debug { "New client connection from ${frontend.remoteAddress()}" }

        val backendFuture = connectToBackend(frontend)
        val backend = backendFuture.channel()

        backendFuture.addListener { future ->
            if (future.isSuccess) {
                logger.debug { "Successfully connected to backend: ${backend.localAddress()} -> ${backend.remoteAddress()}" }
                setupPipelines(frontend, backend)
                establishSession(frontend, backend)
                onConnectionEstablished(frontend, backend)
                frontend.config().isAutoRead = true
                frontend.read()
            } else {
                logger.error(future.cause()) { "Failed to connect to backend: $upstreamHost:$upstreamPort" }
                if (backend != null && backend.isActive) {
                    backend.close()
                }
                frontend.closeOnFlush()
            }
        }

        frontend.pipeline().remove(this)
    }

    private fun connectToBackend(frontend: Channel) =
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
            ).connect(upstreamHost, upstreamPort)

    private fun setupPipelines(
        frontend: Channel,
        backend: Channel,
    ) {
        frontend.attr(GatewayAttrs.BACKEND_ATTR_KEY).set(backend)
        backend.attr(GatewayAttrs.FRONTEND_ATTR_KEY).set(frontend)

        frontend.pipeline().addLast(
            *frontendHandlers.toTypedArray(),
        )
        stateMachine?.let {
            frontend.pipeline().addLast(
                "state-machine-handler",
                StateMachineHandler(it, MessageDirection.FRONTEND),
            )
        }
    }

    private fun establishSession(
        frontend: Channel,
        backend: Channel,
    ) {
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
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}
