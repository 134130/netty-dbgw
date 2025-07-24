package com.github.l34130.netty.dbgw.core

sealed class MessageAction {
    object Forward : MessageAction() {
        override fun toString(): String = "Forward"
    }

    data class Transform(
        val newMsg: Any,
    ) : MessageAction() {
        override fun toString(): String = "Transform(newMsg=$newMsg)"
    }

    data class Intercept(
        val response: Any,
    ) : MessageAction() {
        override fun toString(): String = "Intercept(response=$response)"
    }

    data class Terminate(
        val reason: String?,
    ) : MessageAction() {
        override fun toString(): String = "Terminate(reason='$reason')"
    }
}
