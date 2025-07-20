package com.github.l34130.netty.dbgw.protocol.mysql.constant

import com.github.l34130.netty.dbgw.utils.Flag

enum class CapabilityFlag(
    override val value: ULong,
) : Flag {
    CLIENT_CONNECT_WITH_DB(1UL shl 3),
    CLIENT_PROTOCOL_41(1UL shl 9),
    CLIENT_SSL(1UL shl 11),
    CLIENT_TRANSACTIONS(1UL shl 13),
    CLIENT_SECURE_CONNECTION(1UL shl 15),
    CLIENT_PLUGIN_AUTH(1UL shl 19),
    CLIENT_CONNECT_ATTRS(1UL shl 20),
    CLIENT_PLUGIN_AUTH_LENENC_CLIENT_DATA(1UL shl 21),
    CLIENT_SESSION_TRACK(1UL shl 23),
    CLIENT_DEPRECATE_EOF(1UL shl 24),
    CLIENT_OPTIONAL_RESULTSET_METADATA(1UL shl 25),
    CLIENT_ZSTD_COMPRESSION_ALGORITHM(1UL shl 26),
    CLIENT_QUERY_ATTRIBUTES(1UL shl 27),
}
