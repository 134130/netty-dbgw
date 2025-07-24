package com.github.l34130.netty.dbgw.protocol.postgres.startup

import com.github.l34130.netty.dbgw.protocol.postgres.Message

class BackendKeyData(
    val processId: Int,
    val secretKey: Int,
) {
    override fun toString(): String = "BackendKeyData(processId=$processId, secretKey=$secretKey)"

    companion object {
        fun readFrom(msg: Message): BackendKeyData {
            require(msg.type == 'K') {
                "Expected 'K' for BackendKeyData, but got ${msg.type}"
            }
            val processId = msg.content.readInt()
            val secretKey = msg.content.readInt()
            return BackendKeyData(processId, secretKey)
        }
    }
}
