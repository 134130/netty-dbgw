package com.github.l34130.netty.dbgw.core

enum class MessageDirection {
    FRONTEND,
    BACKEND,
    ;

    fun opposite(): MessageDirection =
        when (this) {
            FRONTEND -> BACKEND
            BACKEND -> FRONTEND
        }
}
