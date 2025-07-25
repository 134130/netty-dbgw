package com.github.l34130.netty.dbgw.core

import com.github.l34130.netty.dbgw.core.config.GatewayConfig
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.util.AttributeKey
import io.netty.util.concurrent.EventExecutorGroup

object GatewayAttrs {
    val BUSINESS_EVENT_EXECUTOR_GROUP_ATTR_KEY: AttributeKey<EventExecutorGroup> = AttributeKey.valueOf("business-executor-group")
    val DOWNSTREAM_ATTR_KEY: AttributeKey<Channel> = AttributeKey.valueOf("downstream")
    val UPSTREAM_ATTR_KEY: AttributeKey<Channel> = AttributeKey.valueOf("upstream")
    val GATEWAY_CONFIG_ATTR_KEY: AttributeKey<GatewayConfig> = AttributeKey.valueOf("config")
}

fun ChannelHandlerContext.businessEventExecutorGroup(): EventExecutorGroup =
    checkNotNull(this.channel().attr(GatewayAttrs.BUSINESS_EVENT_EXECUTOR_GROUP_ATTR_KEY).get()) {
        "Business event executor group is not set in the context"
    }

fun ChannelHandlerContext.downstream(): Channel =
    checkNotNull(this.channel().attr(GatewayAttrs.DOWNSTREAM_ATTR_KEY).get()) {
        "Downstream channel is not set in the context. Maybe trying to access already in the downstream handler?"
    }

fun ChannelHandlerContext.upstream(): Channel =
    checkNotNull(this.channel().attr(GatewayAttrs.UPSTREAM_ATTR_KEY).get()) {
        "Upstream channel is not set in the context. Maybe trying to access already in the upstream handler?"
    }

fun ChannelHandlerContext.gatewayConfig(): GatewayConfig = this.channel().attr(GatewayAttrs.GATEWAY_CONFIG_ATTR_KEY).get()
