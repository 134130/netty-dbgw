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
import java.sql.Date
import java.sql.DriverManager
import java.sql.Time
import java.time.LocalDateTime
import java.util.Properties
import kotlin.test.assertEquals
import kotlin.test.assertIs
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

    private fun createConnection(modifier: (props: Properties) -> Unit = {}): Connection {
        val properties =
            Properties().apply {
                setProperty("user", "testuser")
                setProperty("password", "testpass")
                setProperty("sslMode", "DISABLED")
            }
        modifier(properties)
        return DriverManager.getConnection(
            "jdbc:mysql://localhost:3306/testdb",
            properties,
        )
    }

    @TestFactory
    fun `test SSL connection`(): List<DynamicNode> =
        listOf(
            dynamicTest("test with no SSL") {
                createConnection().use { conn ->
                    assertMySqlConnection(conn)
                }
            },
            dynamicTest("test with SSL") {
                createConnection { props ->
                    props.setProperty("sslMode", "REQUIRED")
                }.use { conn ->
                    assertMySqlConnection(conn)
                }
            },
        )

    @TestFactory
    fun `test connection with different authentication plugins`(): List<DynamicNode> =
        listOf(
            dynamicTest("test with mysql_native_password") {
                createConnection { props ->
                    props.setProperty("defaultAuthenticationPlugin", "mysql_native_password")
                }.use { conn ->
                    assertMySqlConnection(conn)
                }
            },
            dynamicTest("test with caching_sha2_password") {
                createConnection { props ->
                    props.setProperty("defaultAuthenticationPlugin", "caching_sha2_password")
                }.use { conn ->
                    assertMySqlConnection(conn)
                }
            },
        )

    @TestFactory
    fun `test various datatypes`(): List<DynamicNode> =
        listOf(
            dynamicTest("test integer data type") {
                createConnection().use { conn ->
                    val result = conn.executeQuery("SELECT 1 AS int_value")
                    assertEquals("int_value", result[0][0], "Expected column name to be 'int_value'")
                    assertEquals(1L, result[1][0], "Expected integer value to be 1, got $result")
                }
            },
            dynamicTest("test string data type") {
                createConnection().use { conn ->
                    val result = conn.executeQuery("SELECT 'test' AS string_value")
                    assertEquals("string_value", result[0][0], "Expected column name to be 'string_value'")
                    assertEquals("test", result[1][0], "Expected string value to be 'test'")
                }
            },
            dynamicTest("test date data type") {
                createConnection().use { conn ->
                    val result = conn.executeQuery("SELECT CURDATE() AS date_value")
                    assertEquals("date_value", result[0][0], "Expected column name to be 'date_value'")
                    assertIs<Date>(result[1][0], "Expected date value to be a string")
                }
            },
            dynamicTest("test time data type") {
                createConnection().use { conn ->
                    val result = conn.executeQuery("SELECT CURTIME() AS time_value")
                    assertEquals("time_value", result[0][0], "Expected column name to be 'time_value'")
                    assertIs<Time>(result[1][0], "Expected time value to be a string")
                }
            },
            dynamicTest("test timestamp data type") {
                createConnection().use { conn ->
                    val result = conn.executeQuery("SELECT CURRENT_TIMESTAMP() AS timestamp_value")
                    assertEquals("timestamp_value", result[0][0], "Expected column name to be 'timestamp_value'")
                    assertIs<LocalDateTime>(result[1][0], "Expected timestamp value to be a string")
                }
            },
            dynamicTest("test boolean data type") {
                createConnection().use { conn ->
                    val result = conn.executeQuery("SELECT TRUE AS bool_value")
                    assertEquals("bool_value", result[0][0], "Expected column name to be 'bool_value'")
                    assertEquals(1L, result[1][0], "Expected boolean value to be true")
                }
            },
            dynamicTest("test float data type") {
                createConnection().use { conn ->
                    val result = conn.executeQuery("SELECT 1.23 AS float_value")
                    assertEquals("float_value", result[0][0], "Expected column name to be 'float_value'")
                    assertEquals(1.23.toBigDecimal(), result[1][0], "Expected float value to be 1.23")
                }
            },
            dynamicTest("test json data type") {
                createConnection().use { conn ->
                    val result = conn.executeQuery("SELECT JSON_OBJECT('key', 'value') AS json_value")
                    assertEquals("json_value", result[0][0], "Expected column name to be 'json_value'")
                    assertEquals("{\"key\": \"value\"}", result[1][0], "Expected JSON value to be {\"key\": \"value\"}")
                }
            },
        )

    private fun assertMySqlConnection(conn: Connection) {
        assertTrue(conn.isValid(2), "Connection should be valid")

        val result = conn.executeQuery("SELECT 1")
        assert(result.isNotEmpty()) { "Result should not be empty" }
        assertEquals("1", result[1][0], "Expected result to be 1, got ${result[1][0]}")

        assertTrue(conn.isValid(2), "Connection should still be valid after query execution")
    }
}
