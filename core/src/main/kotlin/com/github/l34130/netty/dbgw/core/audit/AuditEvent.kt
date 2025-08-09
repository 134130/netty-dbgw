package com.github.l34130.netty.dbgw.core.audit

sealed interface AuditEvent

data class QueryEvent(
    val query: String,
    val parameters: Map<String, Any?>? = null,
) : AuditEvent
