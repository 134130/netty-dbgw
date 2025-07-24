package com.github.l34130.netty.dbgw.protocol.postgres

import com.github.l34130.netty.dbgw.core.AbstractDatabaseGateway
import com.github.l34130.netty.dbgw.core.DatabaseStateMachine
import com.github.l34130.netty.dbgw.core.config.GatewayConfig
import com.github.l34130.netty.dbgw.protocol.postgres.startup.StartupState
import io.netty.channel.ChannelHandler

class PostgresGateway(
    config: GatewayConfig,
) : AbstractDatabaseGateway(config) {
    override fun createDownstreamHandlers(): List<ChannelHandler> = listOf()

    override fun createUpstreamHandlers(): List<ChannelHandler> = listOf()

    override fun createStateMachine(): DatabaseStateMachine? =
        DatabaseStateMachine(
            StartupState(),
            interceptors =
                listOf(
                    TerminateInterceptor(),
                ),
        )
}
