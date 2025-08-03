package com.github.l34130.netty.dbgw.core

import com.github.l34130.netty.dbgw.core.policy.DatabasePolicyChain
import com.github.l34130.netty.dbgw.policy.api.ClientInfo
import com.github.l34130.netty.dbgw.policy.api.SessionInfo
import com.github.l34130.netty.dbgw.policy.api.database.DatabaseConnectionInfo
import com.github.l34130.netty.dbgw.policy.api.database.DatabaseContext
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.util.AttributeKey

object GatewayAttrs {
    val SESSION_INFO_ATTR_KEY: AttributeKey<SessionInfo> = AttributeKey.newInstance("session-info")
    val CLIENT_INFO_ATTR_KEY: AttributeKey<ClientInfo> = AttributeKey.newInstance("client-info")

    val FRONTEND_ATTR_KEY: AttributeKey<Channel> = AttributeKey.newInstance("frontend")
    val BACKEND_ATTR_KEY: AttributeKey<Channel> = AttributeKey.newInstance("backend")

    val DATABASE_POLICY_CHAIN_ATTR_KEY: AttributeKey<DatabasePolicyChain> = AttributeKey.newInstance("database-policy-chain")
    val DATABASE_CONNECTION_INFO_ATTR_KEY: AttributeKey<DatabaseConnectionInfo> = AttributeKey.newInstance("database-connection-info")
    val DATABASE_CONTEXT_ATTR_KEY: AttributeKey<DatabaseContext> = AttributeKey.newInstance("database-context")
}

fun ChannelHandlerContext.sessionInfo(): SessionInfo =
    checkNotNull(this.channel().attr(GatewayAttrs.SESSION_INFO_ATTR_KEY).get()) {
        "Session info is not set in the context"
    }

fun ChannelHandlerContext.clientInfo(): ClientInfo =
    checkNotNull(this.channel().attr(GatewayAttrs.CLIENT_INFO_ATTR_KEY).get()) {
        "Client info is not set in the context"
    }

fun ChannelHandlerContext.frontend(): Channel =
    checkNotNull(this.channel().attr(GatewayAttrs.FRONTEND_ATTR_KEY).get()) {
        "Frontend channel is not set in the context. Maybe trying to access already in the frontend handler? If so, use ctx.channel() instead"
    }

fun ChannelHandlerContext.backend(): Channel =
    checkNotNull(this.channel().attr(GatewayAttrs.BACKEND_ATTR_KEY).get()) {
        "Backend channel is not set in the context. Maybe trying to access already in the backend handler? If so, use ctx.channel() instead"
    }

fun ChannelHandlerContext.databasePolicyChain(): DatabasePolicyChain? =
    this.channel().attr(GatewayAttrs.DATABASE_POLICY_CHAIN_ATTR_KEY).get()

fun ChannelHandlerContext.databaseConnectionInfo(): DatabaseConnectionInfo? =
    this.channel().attr(GatewayAttrs.DATABASE_CONNECTION_INFO_ATTR_KEY).get()

fun ChannelHandlerContext.databaseCtx(): DatabaseContext? = this.channel().attr(GatewayAttrs.DATABASE_CONTEXT_ATTR_KEY).get()
