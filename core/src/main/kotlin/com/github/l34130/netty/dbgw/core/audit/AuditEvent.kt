package com.github.l34130.netty.dbgw.core.audit

import com.github.l34130.netty.dbgw.policy.api.database.DatabaseAuthenticationEvent
import com.github.l34130.netty.dbgw.policy.api.database.DatabaseContext
import com.github.l34130.netty.dbgw.policy.api.database.query.DatabaseQueryContext

sealed interface AuditEvent {
    val ctx: DatabaseContext
}

data class AuthenticationStartAuditEvent(
    override val ctx: DatabaseContext,
    val evt: DatabaseAuthenticationEvent,
) : AuditEvent

data class AuthenticationResultAuditEvent(
    override val ctx: DatabaseContext,
    val evt: DatabaseAuthenticationEvent,
    val success: Boolean,
    val reason: String? = null,
) : AuditEvent

data class QueryStartAuditEvent(
    override val ctx: DatabaseQueryContext,
) : AuditEvent

data class QueryResultAuditEvent(
    override val ctx: DatabaseContext,
) : AuditEvent
