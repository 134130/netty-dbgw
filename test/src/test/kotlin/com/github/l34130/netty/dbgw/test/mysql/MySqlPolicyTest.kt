package com.github.l34130.netty.dbgw.test.mysql

import com.github.l34130.netty.dbgw.core.policy.PolicyConfigurationLoader
import com.github.l34130.netty.dbgw.policy.api.PolicyDefinition
import com.github.l34130.netty.dbgw.policy.builtin.database.DatabaseImpersonationPolicyDefinition
import com.github.l34130.netty.dbgw.policy.builtin.database.DatabaseResultSetMaskingPolicyDefinition
import com.github.l34130.netty.dbgw.policy.builtin.database.DatabaseRowLevelSecurityPolicyDefinition
import com.github.l34130.netty.dbgw.protocol.mysql.MySqlGateway
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.assertAll
import java.sql.Date
import java.sql.SQLException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

abstract class MySqlPolicyTest(
    private val image: String,
) : MySqlIntegrationTestBase(image) {
    @Test
    fun `test DatabaseImpersonationPolicy`() {
        val gateway =
            MySqlGateway(
                config = createDatabaseGatewayConfig(),
                policyConfigurationLoader =
                    PolicyConfigurationLoader.of(
                        DatabaseImpersonationPolicyDefinition(
                            action =
                                DatabaseImpersonationPolicyDefinition.Action(
                                    user = "testuser",
                                    password = "testpass",
                                ),
                        ),
                        PolicyDefinition.ALLOW_ALL,
                    ),
            )
        gateway.start()

        try {
            gateway
                .createConnection { props ->
                    props.setProperty("user", "invalid_user")
                    props.setProperty("password", "invalid_pass")
                }.use { conn ->
                    val result = conn.executeQuery("SELECT CURRENT_USER()")
                    assertEquals("testuser@%", result[1][0])
                }
        } finally {
            gateway.shutdown()
        }
    }

    @Test
    fun `test DatabaseResultSetMaskingPolicy`() {
        val gateway =
            MySqlGateway(
                config = createDatabaseGatewayConfig(),
                policyConfigurationLoader =
                    PolicyConfigurationLoader.of(
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
                    ),
            )
        gateway.start()

        try {
            gateway.createConnection().use { conn ->
                val stringResult = conn.executeQuery("SELECT '123456789' AS str_col")
                val intResult = conn.executeQuery("SELECT 123456789 AS int_col")
                val dateResult = conn.executeQuery("SELECT DATE '2024-06-09' AS date_col")
                val nullResult = conn.executeQuery("SELECT NULL AS null_col")

                assertAll(
                    { assertEquals("******78*", stringResult[1][0]) },
                    { assertEquals(123456789L, intResult[1][0]) },
                    { assertEquals(Date.valueOf("2024-06-09"), dateResult[1][0]) },
                    { assertEquals(null, nullResult[1][0]) },
                )
            }
        } finally {
            gateway.shutdown()
        }
    }

    @Test
    fun `test DatabaseRowLevelSecurityPolicy`() {
        assumeTrue(image == "mysql:8")

        val gateway =
            MySqlGateway(
                createDatabaseGatewayConfig(),
                policyConfigurationLoader =
                    PolicyConfigurationLoader.of(
                        DatabaseRowLevelSecurityPolicyDefinition(
                            column = ".*id",
                            filter = "[1-5]",
                            action = DatabaseRowLevelSecurityPolicyDefinition.Action.DENY,
                        ),
                        PolicyDefinition.ALLOW_ALL,
                    ),
            )
        gateway.start()

        try {
            gateway.createConnection().use { conn ->
                val result =
                    conn.executeQuery(
                        """
                        SELECT *
                        FROM (
                            VALUES
                                ROW(1,  'Penelope',  'Guiness'),
                                ROW(2,  'Nick',      'Wahlberg'),
                                ROW(3,  'Ed',        'Chase'),
                                ROW(4,  'Jennifer',  'Davis'),
                                ROW(5,  'Johnny',    'Lollobrigida'),
                                ROW(6,  'Bette',     'Nicholson'),
                                ROW(7,  'Grace',     'Mostel'),
                                ROW(8,  'Matthew',   'Johansson'),
                                ROW(9,  'Joe',       'Swank'),
                                ROW(10, 'Christian', 'Gable')
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
