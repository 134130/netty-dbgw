package com.github.l34130.netty.dbgw.app.handler.codec.mysql

enum class CapabilityFlag(
    override val value: Int,
) : Flag {
    CLIENT_CONNECT_WITH_DB(1 shl 3),
    CLIENT_PROTOCOL_41(1 shl 9),
    CLIENT_SSL(1 shl 11),
    CLIENT_TRANSACTIONS(1 shl 13),
    CLIENT_SECURE_CONNECTION(1 shl 15),
    CLIENT_PLUGIN_AUTH(1 shl 19),
    CLIENT_CONNECT_ATTRS(1 shl 20),
    CLIENT_PLUGIN_AUTH_LENENC_CLIENT_DATA(1 shl 21),
    CLIENT_SESSION_TRACK(1 shl 23),
    CLIENT_DEPRECATE_EOF(1 shl 24),
    CLIENT_OPTIONAL_RESULTSET_METADATA(1 shl 25),
    CLIENT_ZSTD_COMPRESSION_ALGORITHM(1 shl 26),
    CLIENT_QUERY_ATTRIBUTES(1 shl 27),
}
