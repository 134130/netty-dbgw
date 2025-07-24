package com.github.l34130.netty.dbgw.protocol.postgres.message

import com.github.l34130.netty.dbgw.protocol.postgres.readUntil
import com.github.l34130.netty.dbgw.protocol.postgres.readUntilNull
import io.netty.buffer.ByteBuf

// For historical reasons, the very first message sent by the client (the startup message) has no initial message-type byte.
data class StartupMessage(
    val majorVersion: Short,
    val minorVersion: Short,
    val user: String,
    val database: String? = null,
    val options: List<String> = emptyList(),
    val replication: String? = null,
    val parameterValue: String? = null,
) {
    companion object {
        fun readFrom(buf: ByteBuf): StartupMessage {
            val length = buf.readInt()
            check(buf.readableBytes() == length - 4) {
                "Invalid length: expected ${length - 4}, got ${buf.readableBytes()} bytes."
            }

            val majorVersion = buf.readShort()
            val minorVersion = buf.readShort()
            check(majorVersion == 3.toShort() && minorVersion == 0.toShort()) {
                "Unsupported PostgreSQL protocol version: $majorVersion.$minorVersion. Only version 3.0 is supported."
            }

            val user = buf.readUntilNull().toString(Charsets.UTF_8)
            val database = buf.readUntilNull().toString(Charsets.UTF_8).takeIf { it.isNotEmpty() }
            val options = mutableListOf<String>()
            val optionsSlice = buf.readUntilNull()
            while (optionsSlice.isReadable) {
                val option = optionsSlice.readUntil(' '.code.toByte(), '\\'.code.toByte())
                if (option.isNotEmpty()) {
                    options.add(option)
                }
            }
            val replication = buf.readUntilNull().toString(Charsets.UTF_8).takeIf { it.isNotEmpty() }
            val parameterValue = buf.readUntilNull().toString(Charsets.UTF_8).takeIf { it.isNotEmpty() }

            return StartupMessage(
                majorVersion = majorVersion,
                minorVersion = minorVersion,
                user = user,
                database = database,
                options = options,
                replication = replication,
                parameterValue = parameterValue,
            )
        }
    }
}
