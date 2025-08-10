package com.github.l34130.netty.dbgw.protocol.postgres.message

import com.github.l34130.netty.dbgw.protocol.postgres.readUntilNull
import com.github.l34130.netty.dbgw.protocol.postgres.writeNullTerminatedString
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufConvertible
import io.netty.buffer.Unpooled

// For historical reasons, the very first message sent by the client (the startup message) has no initial message-type byte.
data class StartupMessage(
    val majorVersion: Short,
    val minorVersion: Short,
    val user: String,
    val database: String? = null,
    val options: List<String> = emptyList(),
    val replication: String? = null,
    val parameterValue: String? = null,
) : ByteBufConvertible {
    override fun asByteBuf(): ByteBuf {
        val buf = Unpooled.buffer()

        // Placeholder for length
        buf.writeInt(0)

        // Protocol version (e.g., 3.0 -> 196608)
        buf.writeInt((majorVersion.toInt() shl 16) or (minorVersion.toInt() and 0xFFFF))

        // Required user
        buf.writeNullTerminatedString("user")
        buf.writeNullTerminatedString(user)

        // Optional database
        database?.let {
            buf.writeNullTerminatedString("database")
            buf.writeNullTerminatedString(it)
        }

        // Options as single string, if any
        if (options.isNotEmpty()) {
            buf.writeNullTerminatedString("options")
            buf.writeNullTerminatedString(options.joinToString(" "))
        }

        // Optional replication
        replication?.let {
            buf.writeNullTerminatedString("replication")
            buf.writeNullTerminatedString(it)
        }

        // Optional parameter value
        parameterValue?.let {
            buf.writeNullTerminatedString("parameter_value")
            buf.writeNullTerminatedString(it)
        }

        // End of parameters
        buf.writeByte(0)

        // Update total length (including length field itself)
        buf.setInt(0, buf.writerIndex())

        return buf
    }

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

            var user: String? = null
            var database: String? = null
            val options = mutableListOf<String>()
            var replication: String? = null
            var parameterValue: String? = null

            // Loop through key/value pairs
            while (true) {
                val key = buf.readUntilNull().toString(Charsets.UTF_8)
                if (key.isEmpty()) break // End marker

                val value = buf.readUntilNull().toString(Charsets.UTF_8)

                when (key) {
                    "user" -> user = value
                    "database" -> database = value
                    "options" -> options.addAll(value.split(" ").filter { it.isNotBlank() })
                    "replication" -> replication = value
                    "parameter_value" -> parameterValue = value
                    else -> {
                        // Unknown key, can be ignored or logged
                    }
                }
            }

            require(user != null) { "StartupMessage missing required 'user' parameter" }

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
