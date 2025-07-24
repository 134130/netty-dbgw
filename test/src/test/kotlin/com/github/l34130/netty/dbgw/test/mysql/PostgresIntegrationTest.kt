package com.github.l34130.netty.dbgw.test.mysql

import org.junit.jupiter.api.Nested

class PostgresIntegrationTest {
    @Nested
    inner class Postgres15 : PostgresProtocolTest("postgres:15")

    @Nested
    inner class Postgres14 : PostgresProtocolTest("postgres:14")

    @Nested
    inner class Postgres13 : PostgresProtocolTest("postgres:13")
}
