package com.github.l34130.netty.dbgw.app.handler.codec.mysql

import com.github.l34130.netty.dbgw.utils.netty.closeOnFlush
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.SimpleChannelInboundHandler

class MySqlProxyChannelInitializer(
    private val upstreamProvider: () -> Pair<String, Int>,
) : ChannelInitializer<Channel>() {
    override fun initChannel(ch: Channel) {
        val downstream = ch
        val stateMachine = GatewayStateMachine()

        downstream
            .pipeline()
            .addFirst("packet-decoder", PacketDecoder())
            .addFirst("packet-encoder", PacketEncoder())
            .addLast(DownstreamConnectionHandler(stateMachine, upstreamProvider))
    }

    class DownstreamConnectionHandler(
        private val stateMachine: GatewayStateMachine,
        private val upstreamProvider: () -> Pair<String, Int>,
    ) : SimpleChannelInboundHandler<Packet>() {
        override fun channelActive(ctx: ChannelHandlerContext) {
            val (inetHost, inetPort) = this.upstreamProvider()

            val upstream =
                Bootstrap()
                    .group(ctx.channel().eventLoop())
                    .channel(ctx.channel().javaClass)
                    .handler(UpstreamConnectionHandler(stateMachine))
                    .connect(inetHost, inetPort)
                    .channel()

            upstream
                .pipeline()
                .addFirst("packet-decoder", PacketDecoder())
                .addFirst("packet-encoder", PacketEncoder())

            val downstream = ctx.channel()
            downstream.attr(GatewayAttributes.UPSTREAM_ATTR_KEY).set(upstream)
            upstream.attr(GatewayAttributes.DOWNSTREAM_ATTR_KEY).set(downstream)

            val capabilities = Capabilities()
            downstream.attr(GatewayAttributes.CAPABILITIES_ATTR_KEY).set(capabilities)
            upstream.attr(GatewayAttributes.CAPABILITIES_ATTR_KEY).set(capabilities)
        }

        override fun channelRead0(
            ctx: ChannelHandlerContext,
            msg: Packet,
        ) {
            stateMachine.processDownstream(ctx, msg)
        }

        override fun exceptionCaught(
            ctx: ChannelHandlerContext,
            cause: Throwable,
        ) {
            logger.error(cause) { "Exception caught in DownstreamConnectionHandler: ${cause.message}" }
            ctx.close()
        }

        override fun channelInactive(ctx: ChannelHandlerContext) {
            if (ctx.upstream().isActive) {
                logger.info { "Downstream channel inactive, closing upstream() channel." }
                ctx.upstream().closeOnFlush()
            }
        }

        override fun channelWritabilityChanged(ctx: ChannelHandlerContext) {
            val canWrite = ctx.channel().isWritable
            ctx.upstream().config().isAutoRead = canWrite
            ctx.fireChannelWritabilityChanged()
        }

        companion object {
            private val logger = KotlinLogging.logger {}
        }
    }

    class UpstreamConnectionHandler(
        private val stateMachine: GatewayStateMachine,
    ) : SimpleChannelInboundHandler<Packet>() {
        override fun channelRead0(
            ctx: ChannelHandlerContext,
            msg: Packet,
        ) {
            stateMachine.processUpstream(ctx, msg)
        }

        override fun exceptionCaught(
            ctx: ChannelHandlerContext,
            cause: Throwable,
        ) {
            logger.error(cause) { "Exception caught in UpstreamConnectionHandler: ${cause.message}" }
            ctx.close()
        }

        override fun channelInactive(ctx: ChannelHandlerContext) {
            if (ctx.downstream().isActive) {
                logger.info { "Upstream channel inactive, closing downstream channel." }
                ctx.downstream().closeOnFlush()
            }
        }

        override fun channelWritabilityChanged(ctx: ChannelHandlerContext) {
            val canWrite = ctx.channel().isWritable
            ctx.downstream().config().isAutoRead = canWrite
            ctx.fireChannelWritabilityChanged()
        }

        companion object {
            private val logger = KotlinLogging.logger {}
        }
    }
}
