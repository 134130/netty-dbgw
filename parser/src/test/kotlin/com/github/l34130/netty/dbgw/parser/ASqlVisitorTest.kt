package com.github.l34130.netty.dbgw.parser

import kotlin.test.Test

class ASqlVisitorTest {
    @Test
    fun testSelect() {
        val sqls =
            listOf(
                "SELECT actor_id, first_name, last_name FROM actor WHERE actor_id = 1",
                "SELECT * FROM actor WHERE (actor_id = 1 OR first_name = 'John') AND last_name = 'Doe'",
                "SELECT a.actor_id, a.first_name, a.last_name FROM actor a WHERE a.actor_id = 1",
                "SELECT a.actor_id, b.film_id, b.title FROM actor a JOIN film_actor fa ON a.actor_id = fa.actor_id JOIN film b ON fa.film_id = b.film_id WHERE a.actor_id = 1",
                // Multi From
                "SELECT a.actor_id, b.film_id, b.title FROM actor a, film_actor fa, film b WHERE a.actor_id = fa.actor_id AND fa.film_id = b.film_id AND a.actor_id = 1",
                // Subquery
                "SELECT a.actor_id, a.first_name, a.last_name FROM actor a WHERE a.actor_id IN (SELECT fa.actor_id FROM film_actor fa WHERE fa.film_id = 1)",
                // Derived Table
                "SELECT dt.actor_id, dt.first_name, dt.last_name FROM (SELECT a.actor_id, a.first_name, a.last_name FROM actor a WHERE a.actor_id = 1) AS dt",
            )

        for (sql in sqls) {
            println("===================================================")
            println("SQL: $sql")
            ASqlVisitor().visit(sql)
        }
    }
}
