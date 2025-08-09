package com.github.l34130.netty.dbgw.core.audit

interface AuditSink {
    fun emit(event: AuditEvent)
}
