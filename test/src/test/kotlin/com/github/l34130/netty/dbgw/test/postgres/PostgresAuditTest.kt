package com.github.l34130.netty.dbgw.test.postgres

import com.github.l34130.netty.dbgw.core.audit.AuditEvent
import com.github.l34130.netty.dbgw.core.audit.AuditSink
import com.github.l34130.netty.dbgw.core.audit.QueryStartAuditEvent
import com.github.l34130.netty.dbgw.core.policy.PolicyConfigurationLoader
import com.github.l34130.netty.dbgw.protocol.postgres.PostgresGateway
import com.github.l34130.netty.dbgw.test.ALLOW_ALL
import com.github.l34130.netty.dbgw.test.mysql.executeQuery
import org.junit.jupiter.api.assertAll
import kotlin.test.Test
import kotlin.test.assertEquals

class PostgresAuditTest : PostgresIntegrationTestBase("postgres:15") {
    @Test
    fun `test QueryStartEvent`() {
        val auditSink =
            object : AuditSink {
                val events = mutableListOf<AuditEvent>()

                override fun emit(event: AuditEvent) {
                    events.add(event)
                }
            }
        val gateway =
            PostgresGateway(
                config = createDatabaseGatewayConfig(),
                policyConfigurationLoader = PolicyConfigurationLoader.ALLOW_ALL,
                auditSink = auditSink,
            )
        gateway.start()

        try {
            gateway.createConnection().use { conn ->
                conn.executeQuery("SELECT 1")
            }

//            gateway
//                .createConnection { props ->
//                    props.setProperty("prepareThreshold", "-1")
//                }.use { conn ->
//                    conn.executeQuery("SELECT CONCAT('Hello, ', 'World!') AS greeting")
//                }

            val auditEvents = auditSink.events.filterIsInstance<QueryStartAuditEvent>()
            assertAll(
                { assertEquals(2, auditEvents.size) },
                { assertEquals("SELECT 1", auditEvents[1].ctx.query) },
//                { assertEquals("SELECT CONCAT('Hello, ', 'World!') AS greeting", auditEvents[7].query) },
            )
        } finally {
            gateway.shutdown()
        }
    }
}
