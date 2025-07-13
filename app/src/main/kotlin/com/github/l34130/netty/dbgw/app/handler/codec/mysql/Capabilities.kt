package com.github.l34130.netty.dbgw.app.handler.codec.mysql

class Capabilities(
    private val flag: Long,
) {
    constructor(lowerBytes: Long, upperBytes: Long) : this(
        (lowerBytes) or (upperBytes shl 32),
    )

    fun hasFlag(flag: Long): Boolean = (this@Capabilities.flag and flag) == flag

    companion object {
        const val CLIENT_CONNECT_WITH_DB = (1L shl 3)
        const val CLIENT_PROTOCOL_41 = (1L shl 9)
        const val CLIENT_SSL = (1L shl 11)
        const val CLIENT_SECURE_CONNECTION = (1L shl 15)
        const val CLIENT_PLUGIN_AUTH = (1L shl 19)
        const val CLIENT_CONNECT_ATTRS = (1L shl 20)
        const val CLIENT_PLUGIN_AUTH_LENENC_CLIENT_DATA = (1L shl 21)
        const val CLIENT_SESSION_TRACK = (1L shl 23)
        const val CLIENT_DEPRECATE_EOF = (1L shl 24)
        const val CLIENT_OPTIONAL_RESULTSET_METADATA = (1L shl 25)
        const val CLIENT_ZSTD_COMPRESSION_ALGORITHM = (1L shl 26)
        const val CLIENT_QUERY_ATTRIBUTES = (1L shl 27)
    }
}
