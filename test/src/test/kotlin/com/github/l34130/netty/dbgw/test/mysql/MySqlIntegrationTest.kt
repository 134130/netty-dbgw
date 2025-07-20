package com.github.l34130.netty.dbgw.test.mysql

import org.junit.jupiter.api.Nested

class MySqlIntegrationTest {
    @Nested
    inner class Mysql8 : MySqlProtocolTest("mysql:8.0")

    @Nested
    inner class MySql5 : MySqlProtocolTest("mysql:5.7")
}
