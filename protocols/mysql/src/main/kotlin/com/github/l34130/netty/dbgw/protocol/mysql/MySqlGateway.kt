package com.github.l34130.netty.dbgw.protocol.mysql

import com.github.l34130.netty.dbgw.core.AbstractGateway
import com.github.l34130.netty.dbgw.core.AbstractGatewayChannelInitializer
import com.github.l34130.netty.dbgw.core.GatewayState
import com.github.l34130.netty.dbgw.core.GatewayStateMachine
import com.github.l34130.netty.dbgw.core.config.GatewayConfig
import com.github.l34130.netty.dbgw.core.security.QueryPolicy
import com.github.l34130.netty.dbgw.core.security.QueryPolicyEngine
import com.github.l34130.netty.dbgw.core.security.QueryPolicyResult
import com.github.l34130.netty.dbgw.protocol.mysql.command.PreparedStatement
import com.github.l34130.netty.dbgw.protocol.mysql.connection.HandshakeState
import io.netty.channel.Channel
import io.netty.channel.ChannelHandler

class MySqlGateway(
    private val config: GatewayConfig,
) : AbstractGateway(config) {
    override fun createChannelInitializer(): ChannelHandler = MySqlGatewayChannelInitializer(config)

    private class MySqlGatewayChannelInitializer(
        config: GatewayConfig,
    ) : AbstractGatewayChannelInitializer<Packet, MySqlGatewayStateMachine>(config) {
        private val capabilities = Capabilities()
        private val preparedStatements: MutableMap<UInt, PreparedStatement> = mutableMapOf()
        private val queryPolicyEngine =
            config.restrictedSqlStatements
                .map { sql ->
                    QueryPolicy { query: String ->
                        if (query.contains(sql, ignoreCase = true)) {
                            QueryPolicyResult(
                                isAllowed = false,
                                reason = "Restricted SQL statement: $sql",
                            )
                        } else {
                            QueryPolicyResult(isAllowed = true)
                        }
                    }
                }.let {
                    QueryPolicyEngine(it)
                }

        override fun createStateMachine(): MySqlGatewayStateMachine = MySqlGatewayStateMachine()

        override fun createMessageDecoder(): ChannelHandler = PacketDecoder()

        override fun createMessageEncoder(): ChannelHandler = PacketEncoder()

        override fun downstreamChannelActive(channel: Channel) {
            channel.attr(MySqlAttrs.CAPABILITIES_ATTR_KEY).set(capabilities)
            channel.attr(MySqlAttrs.PREPARED_STATEMENTS_ATTR_KEY).set(preparedStatements)
            channel.attr(MySqlAttrs.QUERY_POLICY_ENGINE_ATTR_KEY).set(queryPolicyEngine)
        }

        override fun upstreamChannelActive(channel: Channel) {
            channel.attr(MySqlAttrs.CAPABILITIES_ATTR_KEY).set(capabilities)
            channel.attr(MySqlAttrs.PREPARED_STATEMENTS_ATTR_KEY).set(preparedStatements)
            channel.attr(MySqlAttrs.QUERY_POLICY_ENGINE_ATTR_KEY).set(queryPolicyEngine)
        }
    }

    private class MySqlGatewayStateMachine(
        initialState: MySqlGatewayState = HandshakeState(),
    ) : GatewayStateMachine<Packet, GatewayState<Packet>>(initialState)
}
