package com.github.l34130.netty.dbgw.protocol.postgres

import com.github.l34130.netty.dbgw.core.AbstractGateway
import com.github.l34130.netty.dbgw.core.GatewayAttrs
import com.github.l34130.netty.dbgw.core.StateMachine
import com.github.l34130.netty.dbgw.core.audit.AuditSink
import com.github.l34130.netty.dbgw.core.config.DatabaseGatewayConfig
import com.github.l34130.netty.dbgw.core.policy.DatabasePolicyChain
import com.github.l34130.netty.dbgw.core.policy.PolicyConfigurationLoader
import com.github.l34130.netty.dbgw.policy.api.database.DatabaseConnectionInfo
import com.github.l34130.netty.dbgw.policy.api.database.DatabaseContext
import com.github.l34130.netty.dbgw.protocol.postgres.startup.StartupState
import io.netty.channel.Channel
import io.netty.channel.ChannelHandler

class PostgresGateway(
    config: DatabaseGatewayConfig,
    private val policyConfigurationLoader: PolicyConfigurationLoader = PolicyConfigurationLoader.NOOP,
    private val auditSink: AuditSink = AuditSink.NOOP,
) : AbstractGateway(config) {
    override fun createFrontendHandlers(): List<ChannelHandler> = listOf()

    override fun createBackendHandlers(): List<ChannelHandler> = listOf()

    override fun createStateMachine(): StateMachine? =
        StateMachine(
            StartupState(),
            interceptors =
                listOf(
                    TerminateInterceptor(),
                ),
        )

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
            databaseType = "POSTGRESQL",
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
}
