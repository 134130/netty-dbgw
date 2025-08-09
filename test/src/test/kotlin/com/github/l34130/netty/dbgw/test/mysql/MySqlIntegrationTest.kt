package com.github.l34130.netty.dbgw.test.mysql

import org.junit.jupiter.api.Nested

class MySqlIntegrationTest {
    @Nested
    inner class MySql8Protocol : MySqlProtocolTest("mysql:8.0")

    @Nested
    inner class MySql5Protocol : MySqlProtocolTest("mysql:5.7")

    @Nested
    inner class MySql8Policy : MySqlPolicyTest("mysql:8.0")

    @Nested
    inner class MySql5Policy : MySqlPolicyTest("mysql:5.7")
}
