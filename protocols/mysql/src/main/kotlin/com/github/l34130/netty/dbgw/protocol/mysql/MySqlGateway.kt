package com.github.l34130.netty.dbgw.protocol.mysql

import com.github.l34130.netty.dbgw.core.AbstractGateway
import com.github.l34130.netty.dbgw.core.StateMachine
import com.github.l34130.netty.dbgw.core.config.DatabaseGatewayConfig
import com.github.l34130.netty.dbgw.core.frontend
import com.github.l34130.netty.dbgw.core.security.QueryPolicy
import com.github.l34130.netty.dbgw.core.security.QueryPolicyEngine
import com.github.l34130.netty.dbgw.core.security.QueryPolicyResult
import com.github.l34130.netty.dbgw.protocol.mysql.command.PreparedStatement
import com.github.l34130.netty.dbgw.protocol.mysql.connection.HandshakeState
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter

class MySqlGateway(
    config: DatabaseGatewayConfig,
) : AbstractGateway(config) {
    override fun createFrontendHandlers(): List<ChannelHandler> = listOf(PacketEncoder(), PacketDecoder())

    override fun createBackendHandlers(): List<ChannelHandler> =
        listOf(MySqlChannelInitialHandler(config), PacketEncoder(), PacketDecoder())

    override fun createStateMachine(): StateMachine = StateMachine(HandshakeState())

    private inner class MySqlChannelInitialHandler(
        config: DatabaseGatewayConfig,
    ) : ChannelInboundHandlerAdapter() {
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

        override fun channelRead(
            ctx: ChannelHandlerContext,
            msg: Any,
        ) {
            ctx.channel().attr(MySqlAttrs.CAPABILITIES_ATTR_KEY).set(capabilities)
            ctx.channel().attr(MySqlAttrs.PREPARED_STATEMENTS_ATTR_KEY).set(preparedStatements)
            ctx.channel().attr(MySqlAttrs.QUERY_POLICY_ENGINE_ATTR_KEY).set(queryPolicyEngine)

            ctx.frontend().apply {
                attr(MySqlAttrs.CAPABILITIES_ATTR_KEY).set(capabilities)
                attr(MySqlAttrs.PREPARED_STATEMENTS_ATTR_KEY).set(preparedStatements)
                attr(MySqlAttrs.QUERY_POLICY_ENGINE_ATTR_KEY).set(queryPolicyEngine)
            }

            ctx.pipeline().remove(this)
            ctx.fireChannelRead(msg)
        }
    }
}
