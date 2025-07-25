package com.github.l34130.netty.dbgw.core

import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

class DefaultThreadFactory(
    poolName: String,
) : ThreadFactory {
    private val prefix = "$poolName-${poolId.getAndIncrement()}-"
    private val nextId = AtomicInteger()

    override fun newThread(r: Runnable): Thread = Thread(r, "$prefix${nextId.getAndIncrement()}")

    companion object {
        private val poolId = AtomicInteger()
    }
}
