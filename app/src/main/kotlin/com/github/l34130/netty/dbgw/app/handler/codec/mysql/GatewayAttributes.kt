package com.github.l34130.netty.dbgw.app.handler.codec.mysql

import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.util.AttributeKey

object GatewayAttributes {
    val DOWNSTREAM_ATTR_KEY: AttributeKey<Channel> = AttributeKey.valueOf("downstream")
    val UPSTREAM_ATTR_KEY: AttributeKey<Channel> = AttributeKey.valueOf("upstream")
    val CAPABILITIES_ATTR_KEY: AttributeKey<Capabilities> = AttributeKey.valueOf("capabilities")
}

fun ChannelHandlerContext.downstream(): Channel = this.channel().attr(GatewayAttributes.DOWNSTREAM_ATTR_KEY).get()

fun ChannelHandlerContext.upstream(): Channel = this.channel().attr(GatewayAttributes.UPSTREAM_ATTR_KEY).get()

fun ChannelHandlerContext.capabilities(): Capabilities = this.channel().attr(GatewayAttributes.CAPABILITIES_ATTR_KEY).get()
