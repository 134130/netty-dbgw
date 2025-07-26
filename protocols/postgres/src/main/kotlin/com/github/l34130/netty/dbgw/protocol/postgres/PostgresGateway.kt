package com.github.l34130.netty.dbgw.protocol.postgres

import com.github.l34130.netty.dbgw.core.AbstractGateway
import com.github.l34130.netty.dbgw.core.StateMachine
import com.github.l34130.netty.dbgw.core.config.DatabaseGatewayConfig
import com.github.l34130.netty.dbgw.protocol.postgres.startup.StartupState
import io.netty.channel.ChannelHandler

class PostgresGateway(
    config: DatabaseGatewayConfig,
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
}
