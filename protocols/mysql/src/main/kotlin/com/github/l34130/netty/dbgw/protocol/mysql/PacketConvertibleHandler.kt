package com.github.l34130.netty.dbgw.protocol.mysql

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageEncoder

internal class PacketConvertibleHandler : MessageToMessageEncoder<PacketConvertible>() {
    override fun encode(
        ctx: ChannelHandlerContext,
        msg: PacketConvertible,
        out: MutableList<Any?>,
    ) {
        try {
            out += msg.asPacket()
        } catch (e: Exception) {
            // Log the error or handle it as needed
            ctx.fireExceptionCaught(e)
        }
    }
}
