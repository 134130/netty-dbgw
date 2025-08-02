package com.github.l34130.netty.dbgw.test.postgres

import com.github.l34130.netty.dbgw.core.policy.DatabasePolicyChain
import com.github.l34130.netty.dbgw.core.policy.PolicyEngine
import com.github.l34130.netty.dbgw.policy.builtin.database.query.DatabaseTimeRangeAccessPolicy
import com.github.l34130.netty.dbgw.protocol.postgres.PostgresGateway
import com.github.l34130.netty.dbgw.test.ClockUtils
import java.sql.SQLException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PostgresPolicyTest : PostgresProtocolTest("postgres:15") {
    @Test
    fun `test DatabaseTimeRangeAccessPolicy`() {
        val gateway =
            PostgresGateway(
                createDatabaseGatewayConfig()
                    .copy(listenPort = 0)
                    .apply {
                        policyEngine =
                            PolicyEngine(
                                DatabasePolicyChain(
                                    listOf(
                                        DatabaseTimeRangeAccessPolicy.from(
                                            range = "[00:00, 00:01)",
                                            action = DatabaseTimeRangeAccessPolicy.Action.DENY,
                                            clock = ClockUtils.fixed("2023-10-01T00:03:00Z"),
                                        ),
                                    ),
                                ),
                            )
                    },
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
