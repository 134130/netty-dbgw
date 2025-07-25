package com.github.l34130.netty.dbgw.core

import com.github.l34130.netty.dbgw.core.config.GatewayConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelInitializer
import io.netty.util.NettyRuntime
import io.netty.util.concurrent.DefaultEventExecutorGroup

class ProxyConnectionHandler(
    private val config: GatewayConfig,
    private val downstreamHandlers: List<ChannelHandler>,
    private val upstreamHandlers: List<ChannelHandler>,
    private val stateMachine: DatabaseStateMachine?,
) : ChannelInboundHandlerAdapter() {
    override fun channelActive(ctx: ChannelHandlerContext) {
        val downstream = ctx.channel()

        logger.debug { "Connected from downstream: ${downstream.remoteAddress()}" }

        val upstreamFuture =
            Bootstrap()
                .group(downstream.eventLoop())
                .channel(downstream.javaClass)
                .handler(
                    object : ChannelInitializer<Channel>() {
                        override fun initChannel(ch: Channel) {
                            ch.pipeline().addLast(
                                *upstreamHandlers.toTypedArray(),
                            )
                            stateMachine?.let {
                                ch.pipeline().addLast(
                                    "state-machine-handler",
                                    StateMachineHandler(it, MessageDirection.UPSTREAM),
                                )
                            }
                        }
                    },
                ).connect(config.upstreamHost, config.upstreamPort)

        val upstream = upstreamFuture.channel()
        upstreamFuture.addListener { future ->

            logger.debug { "Connected to upstream: ${upstream.remoteAddress()}" }

            downstream.attr(GatewayAttrs.UPSTREAM_ATTR_KEY).set(upstream)
            upstream.attr(GatewayAttrs.DOWNSTREAM_ATTR_KEY).set(downstream)

            downstream.config().isAutoRead = true
            downstream.read()
        }

        downstream.pipeline().addLast(
            *downstreamHandlers.toTypedArray(),
        )
        stateMachine?.let {
            downstream.pipeline().addLast(
                "state-machine-handler",
                StateMachineHandler(it, MessageDirection.DOWNSTREAM),
            )
        }

        val businessEventExecutorGroup = DefaultEventExecutorGroup(NettyRuntime.availableProcessors())
        downstream.attr(GatewayAttrs.BUSINESS_EVENT_EXECUTOR_GROUP_ATTR_KEY).set(businessEventExecutorGroup)
        upstream.attr(GatewayAttrs.BUSINESS_EVENT_EXECUTOR_GROUP_ATTR_KEY).set(businessEventExecutorGroup)

        downstream.attr(GatewayAttrs.GATEWAY_CONFIG_ATTR_KEY).set(config)
        upstream.attr(GatewayAttrs.GATEWAY_CONFIG_ATTR_KEY).set(config)

        downstream.pipeline().remove(this)
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}
