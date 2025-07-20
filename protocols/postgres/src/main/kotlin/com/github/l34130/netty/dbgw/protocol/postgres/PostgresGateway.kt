package com.github.l34130.netty.dbgw.protocol.postgres

import com.github.l34130.netty.dbgw.core.AbstractGateway
import com.github.l34130.netty.dbgw.core.AbstractGatewayChannelInitializer
import com.github.l34130.netty.dbgw.core.GatewayState
import com.github.l34130.netty.dbgw.core.GatewayStateMachine
import com.github.l34130.netty.dbgw.core.config.GatewayConfig
import com.github.l34130.netty.dbgw.protocol.postgres.startup.StartupState
import io.netty.buffer.ByteBuf
import io.netty.channel.Channel
import io.netty.channel.ChannelHandler

class PostgresGateway(
    private val config: GatewayConfig,
) : AbstractGateway(config) {
    override fun createChannelInitializer(): ChannelHandler = PostgresGatewayChannelInitializer(config)

    private class PostgresGatewayChannelInitializer(
        config: GatewayConfig,
    ) : AbstractGatewayChannelInitializer<ByteBuf, PostgresGatewayStateMachine>(config) {
        override fun createStateMachine(): PostgresGatewayStateMachine =
            PostgresGatewayStateMachine(
                initialState = StartupState(),
            )

        override fun createMessageDecoder(): ChannelHandler = MessageDecoder()

        override fun createMessageEncoder(): ChannelHandler = MessageEncoder()

        override fun downstreamChannelActive(channel: Channel) {
        }

        override fun upstreamChannelActive(channel: Channel) {
        }
    }

    private class PostgresGatewayStateMachine(
        initialState: PostgresGatewayState,
    ) : GatewayStateMachine<ByteBuf, GatewayState<ByteBuf>>(initialState)
}
