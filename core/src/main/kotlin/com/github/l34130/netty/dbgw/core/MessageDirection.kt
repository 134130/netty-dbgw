package com.github.l34130.netty.dbgw.core

enum class MessageDirection {
    DOWNSTREAM,
    UPSTREAM,
    ;

    fun opposite(): MessageDirection =
        when (this) {
            DOWNSTREAM -> UPSTREAM
            UPSTREAM -> DOWNSTREAM
        }
}
