package com.github.l34130.netty.dbgw.parser

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import kotlin.test.Test
import kotlin.test.assertEquals

class SqlLineageAnalyzerTest {
    @Test
    @DisplayName("SELECT actor_id, first_name, last_name FROM actor WHERE actor_id = 1")
    fun `test select`() {
        val sql = "SELECT actor_id, first_name, last_name FROM actor WHERE actor_id = 1"
        val parsed = SqlLineageAnalyzer().parse(sql)

        val expected =
            ParseResult(
                selectItems =
                    setOf(
                        DirectColumn(TestUtils.directColumn("actor", "actor_id")),
                        DirectColumn(TestUtils.directColumn("actor", "first_name")),
                        DirectColumn(TestUtils.directColumn("actor", "last_name")),
                    ),
                referencedColumns =
                    setOf(
                        TestUtils.directColumn("actor", "actor_id"),
                    ),
            )

        assertEquals(expected, parsed, TestUtils.debugDump(sql, expected, parsed))
    }

    @Test
    @DisplayName("SELECT * FROM actor WHERE actor_id = 1")
    fun `test select all`() {
        val sql = "SELECT * FROM actor WHERE actor_id = 1"
        val parsed = SqlLineageAnalyzer().parse(sql)

        val expected =
            ParseResult(
                selectItems =
                    setOf(
                        DirectColumn(TestUtils.directColumn("actor", "*")),
                    ),
                referencedColumns =
                    setOf(
                        TestUtils.directColumn("actor", "actor_id"),
                    ),
            )

        assertEquals(expected, parsed, TestUtils.debugDump(sql, expected, parsed))
    }

    @Test
    @DisplayName("SELECT a.actor_id, a.first_name, a.last_name FROM actor a WHERE a.actor_id = 1")
    fun `test select with alias`() {
        val sql = "SELECT a.actor_id, a.first_name, a.last_name FROM actor a WHERE a.actor_id = 1"
        val parsed = SqlLineageAnalyzer().parse(sql)

        val expected =
            ParseResult(
                selectItems =
                    setOf(
                        DirectColumn(TestUtils.directColumn("actor", "actor_id", "a")),
                        DirectColumn(TestUtils.directColumn("actor", "first_name", "a")),
                        DirectColumn(TestUtils.directColumn("actor", "last_name", "a")),
                    ),
                referencedColumns =
                    setOf(
                        TestUtils.directColumn("actor", "actor_id", "a"),
                    ),
            )

        assertEquals(expected, parsed, TestUtils.debugDump(sql, expected, parsed))
    }

    @Test
    @DisplayName(
        "SELECT a.actor_id FROM actor a WHERE (a.actor_id = 1 AND a.first_name = 'John') OR a.last_name = 'Doe'",
    )
    fun `test select with condition`() {
        val sql =
            "SELECT a.actor_id FROM actor a WHERE (a.actor_id = 1 AND a.first_name = 'John') OR a.last_name = 'Doe'"
        val parsed = SqlLineageAnalyzer().parse(sql)

        val expected =
            ParseResult(
                selectItems =
                    setOf(
                        DirectColumn(TestUtils.directColumn("actor", "actor_id", "a")),
                    ),
                referencedColumns =
                    setOf(
                        TestUtils.directColumn("actor", "actor_id", "a"),
                        TestUtils.directColumn("actor", "first_name", "a"),
                        TestUtils.directColumn("actor", "last_name", "a"),
                    ),
            )

        assertEquals(expected, parsed, TestUtils.debugDump(sql, expected, parsed))
    }

    @Test
    @DisplayName(
        "SELECT a.actor_id, fa.film_id FROM actor a JOIN film_actor fa ON fa.actor_id = a.actor_id WHERE fa.film_id = 1",
    )
    fun `test select with join`() {
        val sql =
            "SELECT a.actor_id, fa.film_id FROM actor a JOIN film_actor fa ON fa.actor_id = a.actor_id WHERE fa.film_id = 1"
        val parsed = SqlLineageAnalyzer().parse(sql)

        val expected =
            ParseResult(
                selectItems =
                    setOf(
                        DirectColumn(TestUtils.directColumn("actor", "actor_id", "a")),
                        DirectColumn(TestUtils.directColumn("film_actor", "film_id", "fa")),
                    ),
                referencedColumns =
                    setOf(
                        TestUtils.directColumn("film_actor", "actor_id", "fa"),
                        TestUtils.directColumn("actor", "actor_id", "a"),
                        TestUtils.directColumn("film_actor", "film_id", "fa"),
                    ),
            )

        assertEquals(expected, parsed, TestUtils.debugDump(sql, expected, parsed))
    }

    @Test
    @DisplayName(
        "SELECT a.actor_id, a.first_name, a.last_name FROM actor a JOIN film_actor fa ON fa.actor_id = a.actor_id WHERE fa.film_id IN (1, 2, 3)",
    )
    fun `test select with join and IN`() {
        val sql =
            "SELECT a.actor_id, a.first_name, a.last_name FROM actor a JOIN film_actor fa ON fa.actor_id = a.actor_id WHERE fa.film_id IN (1, 2, 3)"
        val parsed = SqlLineageAnalyzer().parse(sql)

        val expected =
            ParseResult(
                selectItems =
                    setOf(
                        DirectColumn(TestUtils.directColumn("actor", "actor_id", "a")),
                        DirectColumn(TestUtils.directColumn("actor", "first_name", "a")),
                        DirectColumn(TestUtils.directColumn("actor", "last_name", "a")),
                    ),
                referencedColumns =
                    setOf(
                        TestUtils.directColumn("film_actor", "actor_id", "fa"),
                        TestUtils.directColumn("actor", "actor_id", "a"),
                        TestUtils.directColumn("film_actor", "film_id", "fa"),
                    ),
            )

        assertEquals(expected, parsed, TestUtils.debugDump(sql, expected, parsed))
    }

    @Test
    @DisplayName(
        "SELECT a.actor_id, a.first_name, a.last_name FROM (SELECT * FROM actor) a WHERE a.actor_id = 1",
    )
    fun `test select with subquery`() {
        val sql =
            "SELECT a.actor_id, a.first_name, a.last_name FROM (SELECT * FROM actor) a WHERE a.actor_id = 1"
        val parsed = SqlLineageAnalyzer().parse(sql)

        val expected =
            ParseResult(
                selectItems =
                    setOf(
                        DirectColumn(TestUtils.directColumn("actor", "actor_id", "a")),
                        DirectColumn(TestUtils.directColumn("actor", "first_name", "a")),
                        DirectColumn(TestUtils.directColumn("actor", "last_name", "a")),
                    ),
                referencedColumns =
                    setOf(
                        TestUtils.directColumn("actor", "actor_id", "a"),
                    ),
            )

        assertEquals(expected, parsed, TestUtils.debugDump(sql, expected, parsed))
    }

    @Test
    @DisplayName(
        "SELECT a.actor_id FROM actor a WHERE a.actor_id IN (SELECT actor_id FROM actor WHERE actor_id = 1)",
    )
    fun `test select with where subquery`() {
        val sql =
            "SELECT a.actor_id FROM actor a WHERE a.actor_id IN (SELECT actor_id FROM actor WHERE first_name LIKE '%John%')"
        val parsed = SqlLineageAnalyzer().parse(sql)

        val expected =
            ParseResult(
                selectItems =
                    setOf(
                        DirectColumn(TestUtils.directColumn("actor", "actor_id", "a")),
                    ),
                referencedColumns =
                    setOf(
                        TestUtils.directColumn("actor", "actor_id", "a"),
                        TestUtils.directColumn("actor", "actor_id"),
                        TestUtils.directColumn("actor", "first_name"),
                    ),
            )

        assertEquals(expected.selectItems, parsed.selectItems, TestUtils.debugDump(sql, expected, parsed))
        assertEquals(expected.referencedColumns.toSet(), parsed.referencedColumns.toSet(), TestUtils.debugDump(sql, expected, parsed))
    }

    @Test
    @DisplayName(
        "SELECT dt.actor_id, dt.first_name, dt.last_name FROM (SELECT a.actor_id, a.first_name, a.last_name FROM actor a WHERE a.actor_id = 1) dt",
    )
    fun `test select with derived table`() {
        val sql =
            "SELECT dt.actor_id, dt.first_name, dt.last_name FROM (SELECT a.actor_id, a.first_name, a.last_name FROM actor a WHERE a.actor_id = 1) dt"
        val parsed = SqlLineageAnalyzer().parse(sql)

        val expected =
            ParseResult(
                selectItems =
                    setOf(
                        DirectColumn(TestUtils.directColumn("actor", "actor_id", "dt")),
                        DirectColumn(TestUtils.directColumn("actor", "first_name", "dt")),
                        DirectColumn(TestUtils.directColumn("actor", "last_name", "dt")),
                    ),
                referencedColumns =
                    setOf(
                        TestUtils.directColumn("actor", "actor_id", "a"),
                    ),
            )

        assertEquals(expected, parsed, TestUtils.debugDump(sql, expected, parsed))
    }

    @Test
    @DisplayName(
        "SELECT a.actor_id, b.film_id, b.title FROM actor a, film_actor fa, film b WHERE a.actor_id = fa.actor_id AND fa.film_id = b.film_id AND a.actor_id = 1",
    )
    fun `test select with multi from`() {
        val sql =
            "SELECT a.actor_id, b.film_id, b.title FROM actor a, film_actor fa, film b WHERE a.actor_id = fa.actor_id AND fa.film_id = b.film_id AND a.actor_id = 1"
        val parsed = SqlLineageAnalyzer().parse(sql)

        val expected =
            ParseResult(
                selectItems =
                    setOf(
                        DirectColumn(TestUtils.directColumn("actor", "actor_id", "a")),
                        DirectColumn(TestUtils.directColumn("film", "film_id", "b")),
                        DirectColumn(TestUtils.directColumn("film", "title", "b")),
                    ),
                referencedColumns =
                    setOf(
                        TestUtils.directColumn("actor", "actor_id", "a"),
                        TestUtils.directColumn("film_actor", "actor_id", "fa"),
                        TestUtils.directColumn("film_actor", "film_id", "fa"),
                        TestUtils.directColumn("film", "film_id", "b"),
                    ),
            )

        assertEquals(expected, parsed, TestUtils.debugDump(sql, expected, parsed))
    }

    @Test
    @DisplayName(
        "SELECT CONCAT(a.first_name, ' ', a.last_name) FROM actor a WHERE a.actor_id = 1",
    )
    fun `test select with function`() {
        val sql =
            "SELECT CONCAT(a.first_name, ' ', a.last_name) as full_name FROM actor a WHERE a.actor_id = 1"
        val parsed = SqlLineageAnalyzer().parse(sql)

        val expected =
            ParseResult(
                selectItems =
                    setOf(
                        FunctionColumn(
                            functionName = "CONCAT",
                            arguments = listOf("a.first_name", "' '", "a.last_name"),
                            sourceColumns =
                                setOf(
                                    TestUtils.directColumn("actor", "first_name", "a"),
                                    TestUtils.directColumn("actor", "last_name", "a"),
                                ),
                            alias = "full_name",
                        ),
                    ),
                referencedColumns =
                    setOf(
                        TestUtils.directColumn("actor", "actor_id", "a"),
                    ),
            )
        assertEquals(expected, parsed, TestUtils.debugDump(sql, expected, parsed))
    }

    @Test
    @Disabled
    @DisplayName("UPDATE actor SET first_name = 'John' WHERE actor_id = 1")
    fun `test update`() {
        val sql = "UPDATE actor SET first_name = 'John' WHERE actor_id = 1"
        val parsed = SqlLineageAnalyzer().parse(sql)

        val expected =
            ParseResult(
                selectItems = emptySet(),
                referencedColumns =
                    setOf(
                        TestUtils.directColumn("actor", "first_name"),
                        TestUtils.directColumn("actor", "actor_id"),
                    ),
            )

        assertEquals(expected, parsed, TestUtils.debugDump(sql, expected, parsed))
    }

    @Test
    @Disabled
    @DisplayName("INSERT INTO actor (actor_id, first_name, last_name) VALUES (1, 'John', 'Doe')")
    fun `test insert`() {
        val sql = "INSERT INTO actor (actor_id, first_name, last_name) VALUES (1, 'John', 'Doe')"
        val parsed = SqlLineageAnalyzer().parse(sql)

        val expected =
            ParseResult(
                selectItems = emptySet(),
                referencedColumns =
                    setOf(
                        TestUtils.directColumn("actor", "actor_id"),
                        TestUtils.directColumn("actor", "first_name"),
                        TestUtils.directColumn("actor", "last_name"),
                    ),
            )

        assertEquals(expected, parsed, TestUtils.debugDump(sql, expected, parsed))
    }

    @Test
    @DisplayName(
        """
        DELETE FROM orders
        JOIN customers ON orders.customer_id = customers.id
        WHERE customers.is_vip = false
        ORDER BY order_date ASC
        LIMIT 100;
    """,
    )
    fun `test delete`() {
        val sql =
            """
            DELETE FROM orders
            JOIN customers ON orders.customer_id = customers.id
            WHERE customers.is_vip = false
            ORDER BY order_date ASC
            LIMIT 100;
            """.trimIndent()
        val parsed = SqlLineageAnalyzer().parse(sql)

        val expected =
            ParseResult(
                selectItems =
                    setOf(
                        DirectColumn(TestUtils.directColumn("orders", "*")),
                    ),
                referencedColumns =
                    setOf(
                        TestUtils.directColumn("orders", "customer_id"),
                        TestUtils.directColumn("customers", "id"),
                        TestUtils.directColumn("customers", "is_vip"),
                        // The column cannot be resolved in static analysis. It determined in DBMS's runtime.
                        TestUtils.delayedColumn(listOf("orders", "customers"), "order_date"),
                    ),
            )

        assertEquals(expected, parsed, TestUtils.debugDump(sql, expected, parsed))
    }
}
