package com.github.l34130.netty.dbgw.app.handler.codec.mysql

class CapabilitiesFlags(
    private val flags: Long,
) {
    constructor(lowerBytes: Long, upperBytes: Long) : this(
        (lowerBytes) or (upperBytes shl 32),
    )

    fun hasFlag(flag: Long): Boolean = (flags and flag) == flag

    companion object {
        const val CLIENT_CONNECT_WITH_DB = (1L shl 3)
        const val CLIENT_PROTOCOL_41 = (1L shl 9)
        const val CLIENT_SSL = (1L shl 11)
        const val CLIENT_SECURE_CONNECTION = (1L shl 15)
        const val CLIENT_PLUGIN_AUTH = (1L shl 19)
        const val CLIENT_CONNECT_ATTRS = (1L shl 20)
        const val CLIENT_PLUGIN_AUTH_LENENC_CLIENT_DATA = (1L shl 21)
        const val CLIENT_ZSTD_COMPRESSION_ALGORITHM = (1L shl 26)
    }
}
