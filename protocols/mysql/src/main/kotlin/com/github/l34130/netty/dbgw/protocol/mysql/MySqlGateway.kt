package com.github.l34130.netty.dbgw.protocol.mysql

import com.github.l34130.netty.dbgw.core.AbstractGateway
import com.github.l34130.netty.dbgw.core.GatewayAttrs
import com.github.l34130.netty.dbgw.core.StateMachine
import com.github.l34130.netty.dbgw.core.audit.AuditSink
import com.github.l34130.netty.dbgw.core.config.DatabaseGatewayConfig
import com.github.l34130.netty.dbgw.core.frontend
import com.github.l34130.netty.dbgw.core.policy.DatabasePolicyChain
import com.github.l34130.netty.dbgw.core.policy.PolicyConfigurationLoader
import com.github.l34130.netty.dbgw.policy.api.database.DatabaseConnectionInfo
import com.github.l34130.netty.dbgw.policy.api.database.DatabaseContext
import com.github.l34130.netty.dbgw.protocol.mysql.command.PreparedStatement
import com.github.l34130.netty.dbgw.protocol.mysql.connection.HandshakeState
import io.netty.channel.Channel
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter

class MySqlGateway(
    config: DatabaseGatewayConfig,
    private val policyConfigurationLoader: PolicyConfigurationLoader = PolicyConfigurationLoader.NOOP,
    private val auditSink: AuditSink = AuditSink.NOOP,
) : AbstractGateway(config) {
    override fun createFrontendHandlers(): List<ChannelHandler> = listOf(PacketEncoder(), PacketDecoder())

    override fun createBackendHandlers(): List<ChannelHandler> = listOf(MySqlChannelInitialHandler(), PacketEncoder(), PacketDecoder())

    override fun createStateMachine(): StateMachine = StateMachine(HandshakeState())

    override fun onConnectionEstablished(
        frontend: Channel,
        backend: Channel,
    ) {
        auditSink.also { sink ->
            frontend.attr(GatewayAttrs.AUDIT_ATTR_KEY).set(sink)
            backend.attr(GatewayAttrs.AUDIT_ATTR_KEY).set(sink)
        }

        DatabasePolicyChain(policyConfigurationLoader.load().map { it.createPolicy() })
            .also {
                policyConfigurationLoader.watchForChanges(it)
                frontend.attr(GatewayAttrs.DATABASE_POLICY_CHAIN_ATTR_KEY).set(it)
                backend.attr(GatewayAttrs.DATABASE_POLICY_CHAIN_ATTR_KEY).set(it)
            }

        // Set the database connection info attribute for both frontend and backend channels
        DatabaseConnectionInfo(
            databaseType = "MYSQL",
        ).also {
            frontend.attr(GatewayAttrs.DATABASE_CONNECTION_INFO_ATTR_KEY).set(it)
            backend.attr(GatewayAttrs.DATABASE_CONNECTION_INFO_ATTR_KEY).set(it)
        }

        DatabaseContext(
            clientInfo = frontend.attr(GatewayAttrs.CLIENT_INFO_ATTR_KEY).get(),
            connectionInfo = frontend.attr(GatewayAttrs.DATABASE_CONNECTION_INFO_ATTR_KEY).get(),
            sessionInfo = frontend.attr(GatewayAttrs.SESSION_INFO_ATTR_KEY).get(),
        ).also { ctx ->
            frontend.attr(GatewayAttrs.DATABASE_CONTEXT_ATTR_KEY).set(ctx)
            backend.attr(GatewayAttrs.DATABASE_CONTEXT_ATTR_KEY).set(ctx)
        }
    }

    private inner class MySqlChannelInitialHandler : ChannelInboundHandlerAdapter() {
        private val capabilities = Capabilities()
        private val preparedStatements: MutableMap<UInt, PreparedStatement> = mutableMapOf()

        override fun channelRead(
            ctx: ChannelHandlerContext,
            msg: Any,
        ) {
            ctx.channel().attr(MySqlAttrs.CAPABILITIES_ATTR_KEY).set(capabilities)
            ctx.channel().attr(MySqlAttrs.PREPARED_STATEMENTS_ATTR_KEY).set(preparedStatements)

            ctx.frontend().apply {
                attr(MySqlAttrs.CAPABILITIES_ATTR_KEY).set(capabilities)
                attr(MySqlAttrs.PREPARED_STATEMENTS_ATTR_KEY).set(preparedStatements)
            }

            ctx.pipeline().remove(this)
            ctx.fireChannelRead(msg)
        }
    }
}
