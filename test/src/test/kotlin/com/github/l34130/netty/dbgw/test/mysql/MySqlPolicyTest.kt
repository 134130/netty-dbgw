package com.github.l34130.netty.dbgw.test.mysql

import com.github.l34130.netty.dbgw.protocol.mysql.MySqlGateway
import java.sql.SQLException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MySqlPolicyTest : MySqlIntegrationTestBase("mysql:8.0") {
    @Test
    fun `test DatabaseTimeRangeAccessPolicy`() {
        val gateway =
            MySqlGateway(
                createDatabaseGatewayConfig(),
            )
        gateway.start()

        try {
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
