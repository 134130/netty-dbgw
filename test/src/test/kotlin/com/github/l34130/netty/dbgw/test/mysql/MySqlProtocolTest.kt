package com.github.l34130.netty.dbgw.test.mysql

import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import java.sql.Connection
import java.sql.Date
import java.sql.SQLException
import java.sql.Time
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.fail

abstract class MySqlProtocolTest(
    image: String,
) : MySqlIntegrationTestBase(image) {
    protected fun assertMySqlConnection(conn: Connection) {
        assertTrue(conn.isValid(2), "Connection should be valid")

        val result = conn.executeQuery("SELECT 1")
        assert(result.isNotEmpty()) { "Result should not be empty" }
        assertEquals(1L, result[1][0], "Expected result to be 1, got ${result[1][0]}")

        assertTrue(conn.isValid(2), "Connection should still be valid after query execution")
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
            // MySQL 5.7's default is mysql_native_password
            dynamicTest("test with mysql_native_password") {
                createConnection { props ->
                    props.setProperty("defaultAuthenticationPlugin", "mysql_native_password")
                }.use { conn ->
                    assertMySqlConnection(conn)
                }
            },
            // MySQL 8.0+ uses caching_sha2_password by default
            dynamicTest("test with caching_sha2_password") {
                createConnection { props ->
                    props.setProperty("defaultAuthenticationPlugin", "caching_sha2_password")
                }.use { conn ->
                    assertMySqlConnection(conn)
                }
            },
            dynamicTest("test invalid username/password") {
                try {
                    createConnection { props ->
                        props.setProperty("user", "invalid_user")
                        props.setProperty("password", "invalid_pass")
                    }
                } catch (e: SQLException) {
                    assertTrue(
                        e.message?.contains("Access denied for user 'invalid_user'@") == true,
                        "Expected access denied error, got ${e.message}",
                    )
                }
            },
        )

    @TestFactory
    fun `test simple query execution`(): List<DynamicNode> =
        listOf(
            dynamicTest("test simple SELECT query") {
                createConnection().use { conn ->
                    val result = conn.executeQuery("SELECT 1 AS col1")
                    assertEquals("col1", result[0][0], "Expected column name to be 'col1'")
                    assertEquals(1L, result[1][0], "Expected value to be 1, got $result")
                }
            },
            dynamicTest("test multiple rows") {
                createConnection().use { conn ->
                    val result = conn.executeQuery("SELECT 1 AS col1 UNION SELECT 2 AS col1")
                    assertEquals(2, result.size - 1, "Expected 2 rows in the result set")
                    assertEquals("col1", result[0][0], "Expected column name to be 'col1'")
                    assertEquals(1L, result[1][0], "Expected first row value to be 1")
                    assertEquals(2L, result[2][0], "Expected second row value to be 2")
                }
            },
            dynamicTest("test invalid query") {
                createConnection().use { conn ->
                    try {
                        conn.executeQuery("SELECT * FROM non_existent_table")
                    } catch (e: Exception) {
                        assertTrue(
                            e.message?.contains("Table 'testdb.non_existent_table' doesn't exist") == true,
                            "Expected table not found error, got ${e.message}",
                        )
                    }
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

    @TestFactory
    fun `test prepared statements`(): List<DynamicNode> =
        listOf(
            dynamicTest("test prepared statement") {
                createConnection { props ->
                    props.setProperty("useServerPrepStmts", "true")
                }.use { conn ->
                    conn.prepareStatement("SELECT CONCAT(?, ?) AS col1").use { stmt ->
                        stmt.setObject(1, 1)
                        stmt.setObject(2, 2)

                        stmt.executeQuery().use { rs ->
                            val table = rs.readAsTable()
                            assertEquals(2, table.size, "Expected 2 rows in the result set")
                            assertEquals("col1", table[0][0], "Expected column name to be 'col1'")
                            val col1Value = table[1][0]
                            when (col1Value) {
                                is String -> assertEquals("12", col1Value, "Expected concatenated value to be '12'")
                                is ByteArray ->
                                    // MySQL 5.7 returns ByteArray for concatenated values
                                    assertEquals("12", col1Value.toString(Charsets.US_ASCII), "Expected concatenated value to be '12'")
                                else -> fail("Unexpected type for concatenated value: ${col1Value?.javaClass?.name}")
                            }
                        }

                        stmt.setObject(1, "Hello")
                        stmt.setObject(2, " World")

                        stmt.executeQuery().use { rs ->
                            val table = rs.readAsTable()
                            assertEquals(2, table.size, "Expected 2 rows in the result set")
                            assertEquals("col1", table[0][0], "Expected column name to be 'col1'")
                            assertEquals("Hello World", table[1][0], "Expected concatenated value to be 'Hello World'")
                        }
                    }

                    conn.prepareStatement("SELECT ? + ? AS col1").use { stmt ->
                        stmt.setObject(1, 1)
                        stmt.setObject(2, 2)

                        stmt.executeQuery().use { rs ->
                            val table = rs.readAsTable()
                            assertEquals(2, table.size, "Expected 2 rows in the result set")
                            assertEquals("col1", table[0][0], "Expected column name to be 'col1'")
                            assertEquals(3.0, table[1][0], "Expected concatenated value to be 3")
                        }

                        stmt.setObject(1, "Hello")
                        stmt.setObject(2, " World")

                        stmt.executeQuery().use { rs ->
                            val table = rs.readAsTable()
                            assertEquals(2, table.size, "Expected 2 rows in the result set")
                            assertEquals("col1", table[0][0], "Expected column name to be 'col1'")
                            assertEquals("Hello World", table[1][0], "Expected concatenated value to be 'Hello World'")
                        }
                    }
                }
            },
//            dynamicTest("test callable statement") {
//                createConnection { props ->
//                    props.setProperty("useServerPrepStmts", "true")
//                }.use { conn ->
//                    val callableStmt = conn.prepareCall("{CALL my_stored_procedure(?, ?)}")
//                    callableStmt.setObject(1, 1)
//                    callableStmt.setObject(2, 2)
//                    val result = callableStmt.executeQuery()
//                    assertTrue(result.next(), "Result set should not be empty")
//                    assertEquals("12", result.getString(1), "Expected stored procedure result to be '12'")
//                    callableStmt.close()
//                }
//            },
        )

    // TODO: DML/DDL Tests
    // TODO: Transaction Tests
    // TODO: NULL or Empty Row Tests
    // TODO: Stored Procedure Tests
    // TODO: Invalid upstream/database connection handling
}