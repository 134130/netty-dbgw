package com.github.l34130.netty.dbgw.protocol.mysql

import com.github.l34130.netty.dbgw.protocol.mysql.command.PreparedStatement
import io.netty.channel.ChannelHandlerContext
import io.netty.util.AttributeKey

internal object MySqlAttrs {
    val CAPABILITIES_ATTR_KEY: AttributeKey<Capabilities> = AttributeKey.valueOf("capabilities")
    val PREPARED_STATEMENTS_ATTR_KEY: AttributeKey<MutableMap<UInt, PreparedStatement>> = AttributeKey.valueOf("prepared_statements")
}

internal fun ChannelHandlerContext.capabilities(): Capabilities = this.channel().attr(MySqlAttrs.CAPABILITIES_ATTR_KEY).get()

internal fun ChannelHandlerContext.preparedStatements(): MutableMap<UInt, PreparedStatement> =
    this.channel().attr(MySqlAttrs.PREPARED_STATEMENTS_ATTR_KEY).get()
