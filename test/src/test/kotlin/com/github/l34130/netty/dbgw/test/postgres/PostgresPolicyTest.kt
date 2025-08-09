package com.github.l34130.netty.dbgw.test.postgres

import com.github.l34130.netty.dbgw.core.policy.PolicyChangeListener
import com.github.l34130.netty.dbgw.core.policy.PolicyConfigurationLoader
import com.github.l34130.netty.dbgw.policy.api.PolicyDefinition
import com.github.l34130.netty.dbgw.policy.builtin.database.DatabaseResultSetMaskingPolicyDefinition
import com.github.l34130.netty.dbgw.policy.builtin.database.DatabaseRowLevelSecurityPolicyDefinition
import com.github.l34130.netty.dbgw.protocol.postgres.PostgresGateway
import com.github.l34130.netty.dbgw.test.mysql.executeQuery
import org.junit.jupiter.api.assertAll
import java.sql.SQLException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PostgresPolicyTest : PostgresProtocolTest("postgres:15") {
    @Test
    fun `test DatabaseTimeRangeAccessPolicy`() {
        val gateway = PostgresGateway(createDatabaseGatewayConfig())
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

    @Test
    fun `test DatabaseResultSetMaskingPolicy`() {
        val gateway =
            PostgresGateway(
                config = createDatabaseGatewayConfig(),
                policyConfigurationLoader =
                    object : PolicyConfigurationLoader {
                        override fun load(): List<PolicyDefinition> =
                            listOf(
                                DatabaseResultSetMaskingPolicyDefinition(
                                    maskingRegex = "1234",
                                ),
                                DatabaseResultSetMaskingPolicyDefinition(
                                    maskingRegex = "3456",
                                ),
                                DatabaseResultSetMaskingPolicyDefinition(
                                    maskingRegex = "9",
                                ),
                                PolicyDefinition.ALLOW_ALL,
                            )

                        override fun watchForChanges(listener: PolicyChangeListener): AutoCloseable =
                            AutoCloseable {
                                // No-op for this test
                            }
                    },
            )
        gateway.start()

        try {
            createConnection { props ->
                props.setProperty("port", gateway.port().toString())
            }.use { conn ->
                val stringResult = conn.executeQuery("SELECT '123456789' AS str_col")
                val intResult = conn.executeQuery("SELECT 123456789 AS int_col")
                val floatResult = conn.executeQuery("SELECT 123456.789::float AS float_col")
                val dateResult = conn.executeQuery("SELECT DATE '2024-06-09' AS date_col")
                val nullResult = conn.executeQuery("SELECT NULL AS null_col")

                assertAll(
                    { assertEquals("******78*", stringResult[1][0]) },
                    { assertEquals(123456789, intResult[1][0]) },
                    { assertEquals(123456.789, floatResult[1][0]) },
                    { assertEquals(java.sql.Date.valueOf("2024-06-09"), dateResult[1][0]) },
                    { assertEquals(null, nullResult[1][0]) },
                )
            }
        } finally {
            gateway.shutdown()
        }
    }

    @Test
    fun `test DatabaseRowLevelSecurityPolicy`() {
        val gateway =
            PostgresGateway(
                createDatabaseGatewayConfig(),
                policyConfigurationLoader =
                    object : PolicyConfigurationLoader {
                        override fun load(): List<PolicyDefinition> =
                            listOf(
                                DatabaseRowLevelSecurityPolicyDefinition(
                                    column = ".*id",
                                    filter = "[1-5]",
                                    action = DatabaseRowLevelSecurityPolicyDefinition.Action.DENY,
                                ),
                                PolicyDefinition.ALLOW_ALL,
                            )

                        override fun watchForChanges(listener: PolicyChangeListener): AutoCloseable =
                            AutoCloseable {
                                // No-op for this test
                            }
                    },
            )
        gateway.start()

        try {
            createConnection { props ->
                props.setProperty("port", gateway.port().toString())
            }.use { conn ->
                val result =
                    conn.executeQuery(
                        """
                        SELECT *
                        FROM (
                            VALUES
                                (1,  'Penelope',  'Guiness'),
                                (2,  'Nick',      'Wahlberg'),
                                (3,  'Ed',        'Chase'),
                                (4,  'Jennifer',  'Davis'),
                                (5,  'Johnny',    'Lollobrigida'),
                                (6,  'Bette',     'Nicholson'),
                                (7,  'Grace',     'Mostel'),
                                (8,  'Matthew',   'Johansson'),
                                (9,  'Joe',       'Swank'),
                                (10, 'Christian', 'Gable')
                        ) AS actor(actor_id, first_name, last_name)
                        """.trimIndent(),
                    )

                assertAll(
                    { assertEquals(6, result.size) }, // Only rows with id 6-10 should be returned (and metadata row)
                    { assertEquals("Bette", result[1][1]) },
                    { assertEquals("Grace", result[2][1]) },
                    { assertEquals("Matthew", result[3][1]) },
                    { assertEquals("Joe", result[4][1]) },
                    { assertEquals("Christian", result[5][1]) },
                )
            }
        } finally {
            gateway.shutdown()
        }
    }
}
