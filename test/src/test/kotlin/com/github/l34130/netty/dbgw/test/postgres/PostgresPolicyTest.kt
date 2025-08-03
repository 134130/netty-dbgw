package com.github.l34130.netty.dbgw.test.postgres

import com.github.l34130.netty.dbgw.protocol.postgres.PostgresGateway
import java.sql.SQLException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PostgresPolicyTest : PostgresProtocolTest("postgres:15") {
    @Test
    fun `test DatabaseTimeRangeAccessPolicy`() {
        val gateway =
            PostgresGateway(
                createDatabaseGatewayConfig(),
            )
        gateway.start()

        try {
            createConnection()

            val exception =
                assertFailsWith<SQLException> {
                    createConnection { props -> props.setProperty("port", gateway.port().toString()) }
                }
            assertEquals("Access denied: No policy allowed the authentication (implicit deny)", exception.message)
        } finally {
            gateway.shutdown()
        }
    }
}
