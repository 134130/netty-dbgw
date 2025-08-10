package com.github.l34130.netty.dbgw.protocol.postgres

interface MessageConvertible {
    fun asMessage(): Message
}
