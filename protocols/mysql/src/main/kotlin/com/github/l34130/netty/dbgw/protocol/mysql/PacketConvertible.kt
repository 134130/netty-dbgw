package com.github.l34130.netty.dbgw.protocol.mysql

internal interface PacketConvertible {
    fun asPacket(): Packet
}
