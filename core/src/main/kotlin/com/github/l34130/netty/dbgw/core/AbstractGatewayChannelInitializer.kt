package com.github.l34130.netty.dbgw.core

import com.github.l34130.netty.dbgw.core.config.GatewayConfig
import com.github.l34130.netty.dbgw.core.utils.netty.closeOnFlush
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.SimpleChannelInboundHandler

abstract class AbstractGatewayChannelInitializer<T, K : GatewayStateMachine<T>>(
    private val config: GatewayConfig,
) : ChannelInitializer<Channel>() {
    override fun initChannel(ch: Channel) {
        val downstream = ch
        val stateMachine: K = createStateMachine()

        downstream
            .pipeline()
            .addLast(
                createMessageDecoder(),
                createMessageEncoder(),
                DownstreamConnectionHandler(stateMachine),
            )
    }

    protected abstract fun createStateMachine(): K

    protected abstract fun createMessageDecoder(): ChannelHandler

    protected abstract fun createMessageEncoder(): ChannelHandler

    private inner class DownstreamConnectionHandler(
        private val stateMachine: K,
    ) : SimpleChannelInboundHandler<T>() {
        private val logger = KotlinLogging.logger {}

        override fun channelActive(ctx: ChannelHandlerContext) {
            val (inetHost, inetPort) = config.upstreamHost to config.upstreamPort

            val upstream =
                Bootstrap()
                    .group(ctx.channel().eventLoop())
                    .channel(ctx.channel().javaClass)
                    .handler(UpstreamConnectionHandler(stateMachine))
                    .connect(inetHost, inetPort)
                    .channel()
                    .apply {
                        pipeline().addFirst(createMessageDecoder(), createMessageEncoder())
                    }

            val downstream = ctx.channel()
            downstream.attr(GatewayAttrs.UPSTREAM_ATTR_KEY).set(upstream)
            upstream.attr(GatewayAttrs.DOWNSTREAM_ATTR_KEY).set(downstream)

            downstream.attr(GatewayAttrs.GATEWAY_CONFIG_ATTR_KEY).set(config)
            upstream.attr(GatewayAttrs.GATEWAY_CONFIG_ATTR_KEY).set(config)
        }

        override fun channelRead0(
            ctx: ChannelHandlerContext,
            msg: T,
        ) {
            stateMachine.processDownstreamMessage(ctx, msg)
        }

        override fun exceptionCaught(
            ctx: ChannelHandlerContext,
            cause: Throwable,
        ) {
            if (cause is ClosingConnectionException) {
                logger.info { "Closing connection due to: ${cause.message}" }
            } else {
                logger.error(cause) { "Exception caught in downstream: ${cause.message}" }
            }
            ctx.close()
        }

        override fun channelInactive(ctx: ChannelHandlerContext) {
            if (ctx.upstream().isActive) {
                logger.info { "Downstream channel inactive, closing upstream() channel." }
                ctx.upstream().closeOnFlush()
            }
        }

        override fun channelWritabilityChanged(ctx: ChannelHandlerContext) {
            logger.trace { "Channel writability changed in downstream handler." }
            val canWrite = ctx.channel().isWritable
            ctx.upstream().config().isAutoRead = canWrite
            ctx.fireChannelWritabilityChanged()
        }
    }

    private inner class UpstreamConnectionHandler(
        private val stateMachine: GatewayStateMachine<T>,
    ) : SimpleChannelInboundHandler<T>() {
        override fun channelRead0(
            ctx: ChannelHandlerContext,
            msg: T,
        ) {
            stateMachine.processUpstreamMessage(ctx, msg)
        }

        override fun exceptionCaught(
            ctx: ChannelHandlerContext,
            cause: Throwable,
        ) {
            if (cause is ClosingConnectionException) {
                logger.info { "Closing connection due to: ${cause.message}" }
            } else {
                logger.error(cause) { "Exception caught in upstream: ${cause.message}" }
            }
            ctx.close()
        }

        override fun channelInactive(ctx: ChannelHandlerContext) {
            if (ctx.downstream().isActive) {
                logger.info { "Upstream channel inactive, closing downstream channel." }
                ctx.downstream().closeOnFlush()
            }
        }

        override fun channelWritabilityChanged(ctx: ChannelHandlerContext) {
            logger.trace { "Channel writability changed in upstream handler." }
            val canWrite = ctx.channel().isWritable
            ctx.downstream().config().isAutoRead = canWrite
            ctx.fireChannelWritabilityChanged()
        }

        private val logger = KotlinLogging.logger {}
    }
}
