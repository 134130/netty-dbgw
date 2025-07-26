package com.github.l34130.netty.dbgw.core

import com.github.l34130.netty.dbgw.core.config.GatewayConfig
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.util.AttributeKey

object GatewayAttrs {
    val FRONTEND_ATTR_KEY: AttributeKey<Channel> = AttributeKey.valueOf("frontend")
    val BACKEND_ATTR_KEY: AttributeKey<Channel> = AttributeKey.valueOf("backend")
    val GATEWAY_CONFIG_ATTR_KEY: AttributeKey<GatewayConfig> = AttributeKey.valueOf("config")
}

fun ChannelHandlerContext.frontend(): Channel =
    checkNotNull(this.channel().attr(GatewayAttrs.FRONTEND_ATTR_KEY).get()) {
        "Frontend channel is not set in the context. Maybe trying to access already in the frontend handler? If so, use ctx.channel() instead."
    }

fun ChannelHandlerContext.backend(): Channel =
    checkNotNull(this.channel().attr(GatewayAttrs.BACKEND_ATTR_KEY).get()) {
        "Backend channel is not set in the context. Maybe trying to access already in the backend handler? If so, use ctx.channel() instead."
    }

fun ChannelHandlerContext.gatewayConfig(): GatewayConfig = this.channel().attr(GatewayAttrs.GATEWAY_CONFIG_ATTR_KEY).get()
