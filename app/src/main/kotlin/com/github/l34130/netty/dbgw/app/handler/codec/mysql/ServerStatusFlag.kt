package com.github.l34130.netty.dbgw.app.handler.codec.mysql

enum class ServerStatusFlag(
    override val value: Int,
) : Flag {
    SERVER_STATUS_IN_TRANS(1 shl 0),
    SERVER_STATUS_AUTOCOMMIT(1 shl 1),
    SERVER_MORE_RESULTS_EXISTS(1 shl 2),
    SERVER_QUERY_NO_GOOD_INDEX_USED(1 shl 3),
    SERVER_QUERY_NO_INDEX_USED(1 shl 4),
    SERVER_STATUS_CURSOR_EXISTS(1 shl 5),
    SERVER_STATUS_LAST_ROW_SENT(1 shl 6),
    SERVER_STATUS_DB_DROPPED(1 shl 7),
    SERVER_STATUS_NO_BACKSLASH_ESCAPES(1 shl 8),
    SERVER_STATUS_METADATA_CHANGED(1 shl 9),
    SERVER_QUERY_WAS_SLOW(1 shl 10),
    SERVER_PS_OUT_PARAMS(1 shl 11),
    SERVER_STATUS_IN_TRANS_READONLY(1 shl 12),
    SERVER_SESSION_STATE_CHANGED(1 shl 13),
    ;

    companion object {
        private val valuesMap = entries.associateBy(ServerStatusFlag::value)

        fun of(value: Int): ServerStatusFlag =
            valuesMap[value]
                ?: throw IllegalArgumentException("Unknown ServerStatusFlags value: $value")
    }
}
