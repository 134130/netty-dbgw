package com.github.l34130.netty.dbgw.protocol.mysql

import com.github.l34130.netty.dbgw.core.config.GatewayConfig
import com.github.l34130.netty.dbgw.core.security.QueryPolicy
import com.github.l34130.netty.dbgw.core.security.QueryPolicyEngine
import com.github.l34130.netty.dbgw.core.security.QueryPolicyResult
import com.github.l34130.netty.dbgw.core.utils.netty.closeOnFlush
import com.github.l34130.netty.dbgw.protocol.mysql.command.PreparedStatement
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.SimpleChannelInboundHandler

class MySqlProxyChannelInitializer(
    private val config: GatewayConfig,
) : ChannelInitializer<Channel>() {
    override fun initChannel(ch: Channel) {
        val downstream = ch
        val stateMachine = GatewayStateMachine()

        downstream
            .pipeline()
            .addFirst("packet-decoder", PacketDecoder())
            .addFirst("packet-encoder", PacketEncoder())
            .addLast(DownstreamConnectionHandler(stateMachine))
    }

    inner class DownstreamConnectionHandler(
        private val stateMachine: GatewayStateMachine,
    ) : SimpleChannelInboundHandler<Packet>() {
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

            val preparedStatements = mutableMapOf<UInt, PreparedStatement>()
            downstream.attr(GatewayAttributes.PREPARED_STATEMENTS_ATTR_KEY).set(preparedStatements)
            upstream.attr(GatewayAttributes.PREPARED_STATEMENTS_ATTR_KEY).set(preparedStatements)

            // TODO: Initialize QueryPolicyEngine with the appropriate configuration
            val queryPolicyEngine =
                config.restrictedSqlStatements
                    .map { sql ->
                        QueryPolicy { query: String ->
                            if (query.contains(sql, ignoreCase = true)) {
                                QueryPolicyResult(isAllowed = false, reason = "Access to restricted SQL statement: $sql")
                            } else {
                                QueryPolicyResult(isAllowed = true, null)
                            }
                        }
                    }.let { policies ->
                        QueryPolicyEngine(policies)
                    }

            downstream.attr(GatewayAttributes.QUERY_POLICY_ENGINE_ATTR_KEY).set(queryPolicyEngine)
            upstream.attr(GatewayAttributes.QUERY_POLICY_ENGINE_ATTR_KEY).set(queryPolicyEngine)
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
            if (cause !is ClosingConnectionException) {
                logger.error(cause) { "Exception caught in DownstreamConnectionHandler: ${cause.message}" }
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
            val canWrite = ctx.channel().isWritable
            ctx.upstream().config().isAutoRead = canWrite
            ctx.fireChannelWritabilityChanged()
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
            if (cause !is ClosingConnectionException) {
                logger.error(cause) { "Exception caught in UpstreamConnectionHandler: ${cause.message}" }
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
            val canWrite = ctx.channel().isWritable
            ctx.downstream().config().isAutoRead = canWrite
            ctx.fireChannelWritabilityChanged()
        }

        companion object {
            private val logger = KotlinLogging.logger {}
        }
    }
}
