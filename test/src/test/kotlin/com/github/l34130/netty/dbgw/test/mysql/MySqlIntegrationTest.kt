package com.github.l34130.netty.dbgw.test.mysql

import com.github.l34130.netty.dbgw.app.handler.codec.mysql.MySqlGateway
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.sql.Connection
import java.sql.DriverManager
import java.util.Properties
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Testcontainers
class MySqlIntegrationTest {
    @Container
    private val mysqlContainer =
        MySQLContainer("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass")
            .withExposedPorts(3306)

    private lateinit var gateway: MySqlGateway
    private val gatewayPort: Int = 3306

    @BeforeEach
    fun setup() {
        gateway =
            MySqlGateway(
                port = gatewayPort,
                upstream = mysqlContainer.host to mysqlContainer.getMappedPort(3306),
            )
        // Start the gateway before each test
        gateway.start()
    }

    @AfterEach
    fun tearDown() {
        // Stop the gateway after each test
        gateway.stop()
    }

    @TestFactory
    fun `test SSL connection`(): List<DynamicNode> =
        listOf(
            dynamicTest("test with no SSL") {
                DriverManager
                    .getConnection(
                        "jdbc:mysql://localhost:3306/testdb",
                        Properties().apply {
                            setProperty("user", "testuser")
                            setProperty("password", "testpass")
                            setProperty("sslMode", "DISABLED")
                            setProperty("defaultAuthenticationPlugin", "mysql_native_password")
                        },
                    ).use { conn ->
                        assertMySqlConnection(conn)
                    }
            },
            dynamicTest("test with SSL") {
                DriverManager
                    .getConnection(
                        "jdbc:mysql://localhost:3306/testdb",
                        Properties().apply {
                            setProperty("user", "testuser")
                            setProperty("password", "testpass")
                            setProperty("sslMode", "REQUIRED")
                            setProperty("defaultAuthenticationPlugin", "mysql_native_password")
                        },
                    ).use { conn ->
                        assertMySqlConnection(conn)
                    }
            },
        )

    @TestFactory
    fun `test connection with different authentication plugins`(): List<DynamicNode> =
        listOf(
            dynamicTest("test with mysql_native_password") {
                DriverManager
                    .getConnection(
                        "jdbc:mysql://localhost:3306/testdb",
                        Properties().apply {
                            setProperty("user", "testuser")
                            setProperty("password", "testpass")
                            setProperty("sslMode", "DISABLED")
                            setProperty("defaultAuthenticationPlugin", "mysql_native_password")
                        },
                    ).use { conn ->
                        assertMySqlConnection(conn)
                    }
            },
            dynamicTest("test with caching_sha2_password") {
                DriverManager
                    .getConnection(
                        "jdbc:mysql://localhost:3306/testdb",
                        Properties().apply {
                            setProperty("user", "testuser")
                            setProperty("password", "testpass")
                            setProperty("sslMode", "DISABLED")
                            setProperty("defaultAuthenticationPlugin", "caching_sha2_password")
                        },
                    ).use { conn ->
                        assertMySqlConnection(conn)
                    }
            },
        )

    private fun assertMySqlConnection(conn: Connection) {
        assertTrue(conn.isValid(2), "Connection should be valid")

        val result = conn.executeQuery("SELECT 1")
        assert(result.isNotEmpty()) { "Result should not be empty" }
        assertEquals(result[1][0], "1", "Expected result to be 1, got ${result[1][0]}")
    }
}
