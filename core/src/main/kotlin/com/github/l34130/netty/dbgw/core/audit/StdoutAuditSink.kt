package com.github.l34130.netty.dbgw.core.audit

import io.github.oshai.kotlinlogging.KotlinLogging

object StdoutAuditSink : AuditSink {
    private val logger = KotlinLogging.logger {}

    override fun emit(event: AuditEvent) {
        when (event) {
            is QueryStartAuditEvent -> {
                logger.info { "Query: ${event.evt.query}" }
            }
            // Handle other AuditEvent types if needed
            else -> logger.warn { "Unhandled audit event type: $event" }
        }
    }
}
