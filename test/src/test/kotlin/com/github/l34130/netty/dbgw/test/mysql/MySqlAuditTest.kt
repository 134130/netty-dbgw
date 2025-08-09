package com.github.l34130.netty.dbgw.test.mysql

import com.github.l34130.netty.dbgw.core.audit.AuditEvent
import com.github.l34130.netty.dbgw.core.audit.AuditSink
import com.github.l34130.netty.dbgw.core.audit.QueryEvent
import com.github.l34130.netty.dbgw.core.policy.PolicyConfigurationLoader
import com.github.l34130.netty.dbgw.protocol.mysql.MySqlGateway
import com.github.l34130.netty.dbgw.test.ALLOW_ALL
import org.junit.jupiter.api.assertAll
import kotlin.test.Test
import kotlin.test.assertEquals

class MySqlAuditTest : MySqlIntegrationTestBase("mysql:8.0") {
    @Test
    fun `test QueryEvent`() {
        val auditSink =
            object : AuditSink {
                val events = mutableListOf<AuditEvent>()

                override fun emit(event: AuditEvent) {
                    events.add(event)
                }
            }
        val gateway =
            MySqlGateway(
                config = createDatabaseGatewayConfig(),
                policyConfigurationLoader = PolicyConfigurationLoader.ALLOW_ALL,
                auditSink = auditSink,
            )
        gateway.start()

        try {
            gateway.createConnection().use { conn ->
                conn.executeQuery("SELECT 1")
            }

            gateway
                .createConnection { props ->
                    props.setProperty("useServerPrepStmts", "true")
                }.use { conn ->
                    conn.executeQuery("SELECT CONCAT('Hello, ', 'World!') AS greeting")
                }

            val auditEvents = auditSink.events.filterIsInstance<QueryEvent>()
            assertAll(
                { assertEquals(8, auditEvents.size) },
                { assertEquals("SELECT 1", auditEvents[3].query) },
                { assertEquals("SELECT CONCAT('Hello, ', 'World!') AS greeting", auditEvents[7].query) },
            )
        } finally {
            gateway.shutdown()
        }
    }
}
