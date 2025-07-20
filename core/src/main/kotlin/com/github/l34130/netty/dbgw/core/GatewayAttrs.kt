package com.github.l34130.netty.dbgw.core

import com.github.l34130.netty.dbgw.core.config.GatewayConfig
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.util.AttributeKey

object GatewayAttrs {
    val DOWNSTREAM_ATTR_KEY: AttributeKey<Channel> = AttributeKey.valueOf("downstream")
    val UPSTREAM_ATTR_KEY: AttributeKey<Channel> = AttributeKey.valueOf("upstream")
    val GATEWAY_CONFIG_ATTR_KEY: AttributeKey<GatewayConfig> = AttributeKey.valueOf("config")
}

fun ChannelHandlerContext.downstream(): Channel = this.channel().attr(GatewayAttrs.DOWNSTREAM_ATTR_KEY).get()

fun ChannelHandlerContext.upstream(): Channel = this.channel().attr(GatewayAttrs.UPSTREAM_ATTR_KEY).get()

fun ChannelHandlerContext.gwConfig(): GatewayConfig = this.channel().attr(GatewayAttrs.GATEWAY_CONFIG_ATTR_KEY).get()
