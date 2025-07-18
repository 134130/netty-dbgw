package com.github.l34130.netty.dbgw.app.handler.codec.mysql.constant

import com.github.l34130.netty.dbgw.utils.Flag

enum class ServerStatusFlag(
    override val value: ULong,
) : Flag {
    SERVER_STATUS_IN_TRANS(1UL shl 0),
    SERVER_STATUS_AUTOCOMMIT(1UL shl 1),
    SERVER_MORE_RESULTS_EXISTS(1UL shl 2),
    SERVER_QUERY_NO_GOOD_INDEX_USED(1UL shl 3),
    SERVER_QUERY_NO_INDEX_USED(1UL shl 4),
    SERVER_STATUS_CURSOR_EXISTS(1UL shl 5),
    SERVER_STATUS_LAST_ROW_SENT(1UL shl 6),
    SERVER_STATUS_DB_DROPPED(1UL shl 7),
    SERVER_STATUS_NO_BACKSLASH_ESCAPES(1UL shl 8),
    SERVER_STATUS_METADATA_CHANGED(1UL shl 9),
    SERVER_QUERY_WAS_SLOW(1UL shl 10),
    SERVER_PS_OUT_PARAMS(1UL shl 11),
    SERVER_STATUS_IN_TRANS_READONLY(1UL shl 12),
    SERVER_SESSION_STATE_CHANGED(1UL shl 13),
}
