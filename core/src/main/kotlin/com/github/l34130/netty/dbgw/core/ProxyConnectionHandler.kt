package com.github.l34130.netty.dbgw.core

import com.github.l34130.netty.dbgw.core.config.GatewayConfig
import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelInitializer

class ProxyConnectionHandler(
    private val config: GatewayConfig,
    private val downstreamHandlers: List<ChannelHandler>,
    private val upstreamHandlers: List<ChannelHandler>,
    private val stateMachine: DatabaseStateMachine?,
) : ChannelInboundHandlerAdapter() {
    override fun channelActive(ctx: ChannelHandlerContext) {
        val downstream = ctx.channel()
        val upstream =
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
                .channel()

        downstream.attr(GatewayAttrs.UPSTREAM_ATTR_KEY).set(upstream)
        upstream.attr(GatewayAttrs.DOWNSTREAM_ATTR_KEY).set(downstream)

        downstream.pipeline().addLast(
            *downstreamHandlers.toTypedArray(),
        )
        stateMachine?.let {
            downstream.pipeline().addLast(
                "state-machine-handler",
                StateMachineHandler(it, MessageDirection.DOWNSTREAM),
            )
        }

        downstream.pipeline().remove(this)
    }
}
