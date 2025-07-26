package com.github.l34130.netty.dbgw.core

import com.github.l34130.netty.dbgw.core.config.DatabaseGatewayConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelInitializer

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

        frontend.attr(GatewayAttrs.GATEWAY_CONFIG_ATTR_KEY).set(config)
        backend.attr(GatewayAttrs.GATEWAY_CONFIG_ATTR_KEY).set(config)

        frontend.pipeline().remove(this)
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}
