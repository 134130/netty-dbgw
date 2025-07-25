package com.github.l34130.netty.dbgw.core

import io.netty.channel.Channel
import io.netty.util.concurrent.EventExecutor
import kotlin.math.abs

class ChannelBasedEventExecutorChooser(
    private val executors: Array<out EventExecutor>,
) {
    fun choose(channel: Channel): EventExecutor {
        // Use the channel's ID to ensure consistent executor selection for the same channel
        return executors[abs(channel.id().hashCode() % executors.size)]
    }
}
