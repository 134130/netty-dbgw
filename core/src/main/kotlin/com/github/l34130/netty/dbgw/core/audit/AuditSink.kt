package com.github.l34130.netty.dbgw.core.audit

interface AuditSink {
    fun emit(event: AuditEvent)

    companion object {
        val NOOP: AuditSink =
            object : AuditSink {
                override fun emit(event: AuditEvent) {
                    // No-op implementation
                }
            }
    }
}
