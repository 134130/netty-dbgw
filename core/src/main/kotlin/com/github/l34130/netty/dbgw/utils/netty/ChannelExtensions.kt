package com.github.l34130.netty.dbgw.utils.netty

import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener

fun Channel.closeOnFlush(): ChannelFuture =
    if (this.isActive) {
        writeAndFlush(Unpooled.EMPTY_BUFFER)
            .addListener(ChannelFutureListener.CLOSE)
    } else {
        // Already closed, return a completed future
        this.newSucceededFuture()
    }
