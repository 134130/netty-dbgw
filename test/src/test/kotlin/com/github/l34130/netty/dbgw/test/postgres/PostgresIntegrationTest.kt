package com.github.l34130.netty.dbgw.test.postgres

import org.junit.jupiter.api.Nested

class PostgresIntegrationTest {
    @Nested
    inner class Postgres15Protocol : PostgresProtocolTest("postgres:15")

    @Nested
    inner class Postgres14Protocol : PostgresProtocolTest("postgres:14")

    @Nested
    inner class Postgres13Protocol : PostgresProtocolTest("postgres:13")

    @Nested
    inner class Postgres15Policy : PostgresPolicyTest("postgres:15")

    @Nested
    inner class Postgres14Policy : PostgresPolicyTest("postgres:14")

    @Nested
    inner class Postgres13Policy : PostgresPolicyTest("postgres:13")
}
