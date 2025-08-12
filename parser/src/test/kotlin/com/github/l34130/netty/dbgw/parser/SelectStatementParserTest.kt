package com.github.l34130.netty.dbgw.parser

import com.github.l34130.netty.dbgw.common.database.ColumnDefinition
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import kotlin.test.assertContentEquals

/**
 *  SELECT
 *                 u.id,
 *                 u.name,
 *                 COUNT(o.id) as order_count,
 *                 MAX(o.total) as max_order
 *             FROM users u
 *             LEFT JOIN orders o ON u.id = o.user_id
 *             WHERE u.status = 'active'
 *             GROUP BY u.id, u.name
 *             HAVING COUNT(o.id) > 5
 *             ORDER BY max_order DESC
 */

class SelectStatementParserTest {
    @TestFactory
    fun `test`(): List<DynamicTest?> =
        listOf(
            dynamicTest("SELECT id, name FROM users WHERE age > 30") {
                val sql = "SELECT id, name FROM users WHERE age > 30"

                val parser = DefaultSqlParser()
                val columnDefinitions = parser.parse(sql)

                assertContentEquals(
                    listOf(
//                        ColumnInfo(
//
//                        )
                        ColumnDefinition(
                            table = null,
                            column = "id",
                            orgTables = listOf("users"),
                            orgColumns = listOf("id"),
                        ),
                        ColumnDefinition(
                            table = null,
                            column = "name",
                            orgTables = listOf("users"),
                            orgColumns = listOf("name"),
                        ),
                        ColumnDefinition(
                            table = null,
                            column = null,
                            orgTables = listOf("users"),
                            orgColumns = listOf("age"),
                        ),
                    ),
                    columnDefinitions,
                )
            },
            dynamicTest("SELECT u.id, u.name FROM users u") {
                val sql = "SELECT u.id, u.name FROM users u"

                val parser = DefaultSqlParser()
                val columnDefinitions = parser.parse(sql)

                assertContentEquals(
                    listOf(
                        ColumnDefinition(
                            table = "u",
                            column = "id",
                            orgTables = listOf("users"),
                            orgColumns = listOf("id"),
                        ),
                        ColumnDefinition(
                            table = "u",
                            column = "name",
                            orgTables = listOf("users"),
                            orgColumns = listOf("name"),
                        ),
                    ),
                    columnDefinitions,
                )
            },
            dynamicTest("SELECT * FROM users") {
                val sql = "SELECT * FROM users"

                val parser = DefaultSqlParser()
                val columnDefinitions = parser.parse(sql)

                assertContentEquals(
                    listOf(
                        ColumnDefinition(
                            table = "users",
                            column = "*",
                            orgTables = listOf("users"),
                            orgColumns = listOf("*"),
                        ),
                    ),
                    columnDefinitions,
                )
            },
            dynamicTest("SELECT COUNT(id) AS cnt FROM users") {
                val sql = "SELECT COUNT(id) AS cnt FROM users"

                val parser = DefaultSqlParser()
                val columnDefinitions = parser.parse(sql)

                assertContentEquals(
                    listOf(
                        ColumnDefinition(
                            table = null,
                            column = "cnt",
                            orgTables = listOf("users"),
                            orgColumns = listOf("id"),
                        ),
                    ),
                    columnDefinitions,
                )
            },
            dynamicTest("SELECT u.id, o.amount FROM users u JOIN orders o ON u.id = o.user_id") {
                val sql = "SELECT u.id, o.amount FROM users u JOIN orders o ON u.id = o.user_id"

                val parser = DefaultSqlParser()
                val columnDefinitions = parser.parse(sql)

                assertContentEquals(
                    listOf(
                        ColumnDefinition(
                            table = "u",
                            column = "id",
                            orgTables = listOf("users"),
                            orgColumns = listOf("id"),
                        ),
                        ColumnDefinition(
                            table = "o",
                            column = "amount",
                            orgTables = listOf("orders"),
                            orgColumns = listOf("amount"),
                        ),
                    ),
                    columnDefinitions,
                )
            },
            dynamicTest("SELECT id, o.amount FROM users u LEFT JOIN orders o ON u.id = o.user_id") {
                val sql = "SELECT id, o.amount FROM users u LEFT JOIN orders o ON u.id = o.user_id"

                val parser = DefaultSqlParser()
                val columnDefinitions = parser.parse(sql)

                assertContentEquals(
                    listOf(
                        ColumnDefinition(
                            table = null,
                            column = "id",
                            orgTables = listOf("users"),
                            orgColumns = listOf("id"),
                        ),
                        ColumnDefinition(
                            table = "o",
                            column = "amount",
                            orgTables = listOf("orders"),
                            orgColumns = listOf("amount"),
                        ),
                    ),
                    columnDefinitions,
                )
            },
            dynamicTest("WITH sub AS (SELECT id, name FROM users) SELECT s.name FROM sub s") {
                val sql = "WITH sub AS (SELECT id, name FROM users) SELECT s.name FROM sub s"

                val parser = DefaultSqlParser()
                val columnDefinitions = parser.parse(sql)

                assertContentEquals(
                    listOf(
                        ColumnDefinition(
                            table = "s",
                            column = "name",
                            orgTables = listOf("users"),
                            orgColumns = listOf("name"),
                        ),
                    ),
                    columnDefinitions,
                )
            },
            dynamicTest("SELECT t.name FROM (SELECT id, name FROM users) t") {
                val sql = "SELECT t.name FROM (SELECT id, name FROM users) t"

                val parser = DefaultSqlParser()
                val columnDefinitions = parser.parse(sql)

                assertContentEquals(
                    listOf(
                        ColumnDefinition(
                            table = "t",
                            column = "name",
                            orgTables = listOf("users"),
                            orgColumns = listOf("name"),
                        ),
                    ),
                    columnDefinitions,
                )
            },
            dynamicTest("SELECT (SELECT MAX(amount) FROM orders o WHERE o.user_id = u.id) AS max_amt FROM users u") {
                val sql = "SELECT (SELECT MAX(amount) FROM orders o WHERE o.user_id = u.id) AS max_amt FROM users u"

                val parser = DefaultSqlParser()
                val columnDefinitions = parser.parse(sql)

                assertContentEquals(
                    listOf(
                        ColumnDefinition(
                            table = null,
                            column = "max_amt",
                            orgTables = listOf("orders"),
                            orgColumns = listOf("amount"),
                        ),
                    ),
                    columnDefinitions,
                )
            },
            dynamicTest("SELECT u.name AS username FROM users u") {
                val sql = "SELECT u.name AS username FROM users u"

                val parser = DefaultSqlParser()
                val columnDefinitions = parser.parse(sql)

                assertContentEquals(
                    listOf(
                        ColumnDefinition(
                            table = "u",
                            column = "username",
                            orgTables = listOf("users"),
                            orgColumns = listOf("name"),
                        ),
                    ),
                    columnDefinitions,
                )
            },
            dynamicTest("SELECT u.id, p.id AS pid FROM users u JOIN posts p ON u.id = p.user_id") {
                val sql = "SELECT u.id, p.id AS pid FROM users u JOIN posts p ON u.id = p.user_id"

                val parser = DefaultSqlParser()
                val columnDefinitions = parser.parse(sql)

                assertContentEquals(
                    listOf(
                        ColumnDefinition(
                            table = "u",
                            column = "id",
                            orgTables = listOf("users"),
                            orgColumns = listOf("id"),
                        ),
                        ColumnDefinition(
                            table = null,
                            column = "pid",
                            orgTables = listOf("posts"),
                            orgColumns = listOf("id"),
                        ),
                    ),
                    columnDefinitions,
                )
            },
            dynamicTest("SELECT u.country, COUNT(*) FROM users u GROUP BY u.country") {
                val sql = "SELECT u.country, COUNT(*) FROM users u GROUP BY u.country"

                val parser = DefaultSqlParser()
                val columnDefinitions = parser.parse(sql)

                assertContentEquals(
                    listOf(
                        ColumnDefinition(
                            table = "u",
                            column = "country",
                            orgTables = listOf("users"),
                            orgColumns = listOf("country"),
                        ),
                        ColumnDefinition(
                            table = null,
                            column = "COUNT(*)",
                            orgTables = listOf("users"),
                            orgColumns = listOf("*"),
                        ),
                    ),
                    columnDefinitions,
                )
            },
            dynamicTest("SELECT DISTINCT country FROM users") {
                val sql = "SELECT DISTINCT country FROM users"

                val parser = DefaultSqlParser()
                val columnDefinitions = parser.parse(sql)

                assertContentEquals(
                    listOf(
                        ColumnDefinition(
                            table = null,
                            column = "country",
                            orgTables = listOf("users"),
                            orgColumns = listOf("country"),
                        ),
                    ),
                    columnDefinitions,
                )
            },
            dynamicTest("SELECT u.id + 1 AS uid_next FROM users u") {
                val sql = "SELECT u.id + 1 AS uid_next FROM users u"

                val parser = DefaultSqlParser()
                val columnDefinitions = parser.parse(sql)

                assertContentEquals(
                    listOf(
                        ColumnDefinition(
                            table = null,
                            column = "uid_next",
                            orgTables = listOf("users"),
                            orgColumns = listOf("id"),
                        ),
                    ),
                    columnDefinitions,
                )
            },
        )
}
