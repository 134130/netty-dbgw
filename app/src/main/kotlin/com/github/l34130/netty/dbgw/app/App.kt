package com.github.l34130.app.com.github.l34130.netty.dbgw.app

import com.github.l34130.netty.dbgw.app.handler.codec.mysql.MySqlGateway

fun main() {
    val gateway =
        MySqlGateway(
            port = 3306,
            upstream = Pair("mysql.querypie.io", 3307),
        )
    try {
        gateway.start()
    } finally {
        gateway.shutdown()
    }
}
