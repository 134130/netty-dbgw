package com.github.l34130.netty.dbgw.protocol.postgres

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageEncoder

class MessageConvertibleHandler : MessageToMessageEncoder<MessageConvertible>() {
    override fun encode(
        ctx: ChannelHandlerContext,
        msg: MessageConvertible,
        out: MutableList<Any>,
    ) {
        try {
            out += msg.asMessage()
        } catch (e: Exception) {
            // Log the error or handle it as needed
            ctx.fireExceptionCaught(e)
        }
    }
}
