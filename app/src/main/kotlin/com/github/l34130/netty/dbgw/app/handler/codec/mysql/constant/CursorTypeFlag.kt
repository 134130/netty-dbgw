package com.github.l34130.netty.dbgw.app.handler.codec.mysql.constant

import com.github.l34130.netty.dbgw.utils.Flag

enum class CursorTypeFlag(
    override val value: ULong,
) : Flag {
    CURSOR_TYPE_NO_CURSOR(0UL),
    CURSOR_TYPE_READ_ONLY(1UL),
    CURSOR_TYPE_FOR_UPDATE(2UL),
    CURSOR_TYPE_SCROLLABLE(3UL),
    PARAMETER_COUNT_AVAILABLE(3UL),
}
