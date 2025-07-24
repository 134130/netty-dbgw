package com.github.l34130.netty.dbgw.protocol.postgres.command

import com.github.l34130.netty.dbgw.protocol.postgres.Message

class ReadyForQuery(
    val transactionStatus: TransactionStatus,
) {
    override fun toString(): String = "ReadyForQuery(transactionStatus='${transactionStatus.description}')"

    companion object {
        const val TYPE: Char = 'Z'

        fun readFrom(msg: Message): ReadyForQuery {
            require(msg.type == TYPE) { "Expected $TYPE, but got ${msg.type}" }
            val content = msg.content
            val transactionStatusByte = content.readByte()
            val transactionStatus = TransactionStatus.of(transactionStatusByte)
            return ReadyForQuery(transactionStatus)
        }
    }

    enum class TransactionStatus(
        val status: Char,
        val description: String,
    ) {
        IDLE('I', "idle (not in a transaction block)"),
        IN_TRANSACTION('T', "in a transaction block"),
        FAILED_TRANSACTION('E', "in a failed transaction block"),
        ;

        companion object {
            fun of(byte: Byte): TransactionStatus = of(byte.toInt().toChar())

            fun of(status: Char): TransactionStatus =
                entries.find { it.status == status }
                    ?: throw IllegalArgumentException("Unknown transaction status: '$status'")
        }
    }
}
