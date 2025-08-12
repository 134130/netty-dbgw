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
                listOf(
                    TestUtils.column("actor", "actor_id"),
                    TestUtils.column("actor", "first_name"),
                    TestUtils.column("actor", "last_name"),
                ),
                listOf(
                    TestUtils.column("actor", "actor_id"),
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
                listOf(
                    TestUtils.column("actor", "*"),
                ),
                listOf(
                    TestUtils.column("actor", "actor_id"),
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
                listOf(
                    TestUtils.column("actor", "actor_id", "a"),
                    TestUtils.column("actor", "first_name", "a"),
                    TestUtils.column("actor", "last_name", "a"),
                ),
                listOf(
                    TestUtils.column("actor", "actor_id", "a"),
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
                listOf(
                    TestUtils.column("actor", "actor_id", "a"),
                ),
                listOf(
                    TestUtils.column("actor", "actor_id", "a"),
                    TestUtils.column("actor", "first_name", "a"),
                    TestUtils.column("actor", "last_name", "a"),
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
                listOf(
                    TestUtils.column("actor", "actor_id", "a"),
                    TestUtils.column("film_actor", "film_id", "fa"),
                ),
                listOf(
                    TestUtils.column("film_actor", "actor_id", "fa"),
                    TestUtils.column("actor", "actor_id", "a"),
                    TestUtils.column("film_actor", "film_id", "fa"),
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
                listOf(
                    TestUtils.column("actor", "actor_id", "a"),
                    TestUtils.column("actor", "first_name", "a"),
                    TestUtils.column("actor", "last_name", "a"),
                ),
                listOf(
                    TestUtils.column("film_actor", "actor_id", "fa"),
                    TestUtils.column("actor", "actor_id", "a"),
                    TestUtils.column("film_actor", "film_id", "fa"),
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
                listOf(
                    TestUtils.column("actor", "actor_id", "a"),
                    TestUtils.column("actor", "first_name", "a"),
                    TestUtils.column("actor", "last_name", "a"),
                ),
                listOf(
                    TestUtils.column("actor", "actor_id", "a"),
                ),
            )

        assertEquals(expected, parsed, TestUtils.debugDump(sql, expected, parsed))
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
                listOf(
                    TestUtils.column("actor", "actor_id", "dt"),
                    TestUtils.column("actor", "first_name", "dt"),
                    TestUtils.column("actor", "last_name", "dt"),
                ),
                listOf(
                    TestUtils.column("actor", "actor_id", "a"),
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
                listOf(
                    TestUtils.column("actor", "actor_id", "a"),
                    TestUtils.column("film", "film_id", "b"),
                    TestUtils.column("film", "title", "b"),
                ),
                listOf(
                    TestUtils.column("actor", "actor_id", "a"),
                    TestUtils.column("film_actor", "actor_id", "fa"),
                    TestUtils.column("film_actor", "film_id", "fa"),
                    TestUtils.column("film", "film_id", "b"),
                    TestUtils.column("actor", "actor_id", "a"),
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
                columns = emptyList(),
                referencedColumns =
                    listOf(
                        TestUtils.column("actor", "first_name"),
                        TestUtils.column("actor", "actor_id"),
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
                columns = emptyList(),
                referencedColumns =
                    listOf(
                        TestUtils.column("actor", "actor_id"),
                        TestUtils.column("actor", "first_name"),
                        TestUtils.column("actor", "last_name"),
                    ),
            )

        assertEquals(expected, parsed, TestUtils.debugDump(sql, expected, parsed))
    }

    @Test
    @Disabled
    @DisplayName("DELETE FROM actor WHERE actor_id = 1")
    fun `test delete`() {
        val sql = "DELETE FROM actor WHERE actor_id = 1"
        val parsed = SqlLineageAnalyzer().parse(sql)

        val expected =
            ParseResult(
                columns = emptyList(),
                referencedColumns =
                    listOf(
                        TestUtils.column("actor", "actor_id"),
                    ),
            )

        assertEquals(expected, parsed, TestUtils.debugDump(sql, expected, parsed))
    }
}
