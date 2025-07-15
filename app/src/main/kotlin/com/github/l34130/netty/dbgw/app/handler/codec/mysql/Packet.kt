@file:OptIn(ExperimentalStdlibApi::class)

package com.github.l34130.netty.dbgw.app.handler.codec.mysql

import com.github.l34130.netty.dbgw.app.handler.codec.mysql.constant.CapabilityFlag
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.constant.ServerStatusFlag
import com.github.l34130.netty.dbgw.utils.netty.peek
import com.github.l34130.netty.dbgw.utils.toEnumSet
import io.netty.buffer.ByteBuf
import java.util.EnumSet

class Packet(
    val sequenceId: Int,
    val payload: ByteBuf,
) {
    override fun toString(): String = "Packet(sequenceId=$sequenceId, payload=${payload.readableBytes()} bytes)"

    fun isErrorPacket(): Boolean {
        val firstByte = payload.peek { it.readFixedLengthInteger(1) }
        return firstByte == 0xFFL
    }

    fun isOkPacket(): Boolean {
        val firstByte = payload.peek { it.readFixedLengthInteger(1) }
        return firstByte == 0x00L ||
            firstByte == 0xFEL // 0xFE is used for OK packets with warnings
    }

    fun isEofPacket(): Boolean {
        val firstByte = payload.peek { it.readFixedLengthInteger(1) }
        return firstByte == 0xFEL
    }

    // https://dev.mysql.com/doc/dev/mysql-server/latest/page_protocol_basic_eof_packet.html
    data class Eof(
        val warnings: Int?,
        val statusFlags: EnumSet<ServerStatusFlag>?,
    ) {
        override fun toString(): String = "Packet.Eof(warnings=$warnings, statusFlags=$statusFlags)"

        companion object {
            fun readFrom(
                byteBuf: ByteBuf,
                capabilities: EnumSet<CapabilityFlag>,
            ): Eof {
                val firstByte = byteBuf.readFixedLengthInteger(1)
                if (firstByte != 0xFEL) {
                    throw IllegalStateException("The packet is not an EOF packet, first byte: ${firstByte.toHexString()}")
                }

                var warnings: Int? = null
                var statusFlags: EnumSet<ServerStatusFlag>? = null
                if (capabilities.contains(CapabilityFlag.CLIENT_PROTOCOL_41)) {
                    warnings = byteBuf.readFixedLengthInteger(2).toInt()
                    statusFlags = byteBuf.readFixedLengthInteger(2).toInt().toEnumSet()
                }

                return Eof(warnings, statusFlags)
            }
        }
    }

    // https://dev.mysql.com/doc/dev/mysql-server/latest/page_protocol_basic_err_packet.html
    data class Error(
        val errorCode: Int,
        val sqlStateMarker: String?,
        val sqlState: String?,
        val message: String,
    ) {
        override fun toString(): String =
            "Packet.Error(errorCode=$errorCode, sqlStateMarker=$sqlStateMarker, sqlState=$sqlState, message='$message')"

        companion object {
            fun readFrom(
                byteBuf: ByteBuf,
                capabilities: EnumSet<CapabilityFlag>,
            ): Error {
                val firstByte = byteBuf.readFixedLengthInteger(1)
                if (firstByte != 0xFFL) {
                    throw IllegalStateException("The packet is not an error packet, first byte: ${firstByte.toHexString()}")
                }

                val errorCode = byteBuf.readFixedLengthInteger(2).toInt()
                var sqlStateMarker: String? = null
                var sqlState: String? = null
                if (capabilities.contains(CapabilityFlag.CLIENT_PROTOCOL_41)) {
                    sqlStateMarker = byteBuf.readFixedLengthString(1)
                    sqlState = byteBuf.readFixedLengthString(5)
                }
                val message = byteBuf.readRestOfPacketString().toString(Charsets.UTF_8)

                return Error(
                    errorCode = errorCode,
                    sqlStateMarker = sqlStateMarker,
                    sqlState = sqlState,
                    message = message,
                )
            }
        }
    }

    // https://dev.mysql.com/doc/dev/mysql-server/latest/page_protocol_basic_ok_packet.html
    data class Ok(
        val affectedRows: Long,
        val lastInsertId: Long,
        val statusFlags: EnumSet<ServerStatusFlag>?,
        val warnings: Int?,
        /**
         * Human-readable status information.
         */
        val info: String?,
    ) {
        override fun toString(): String =
            "Packet.Ok(" +
                "affectedRows=$affectedRows, " +
                "lastInsertId=$lastInsertId, " +
                "statusFlags=$statusFlags, " +
                "warnings=$warnings, " +
                "info=$info" +
                ")"

        companion object {
            fun readFrom(
                byteBuf: ByteBuf,
                capabilities: EnumSet<CapabilityFlag>,
            ): Ok {
                val firstByte = byteBuf.readFixedLengthInteger(1)
                if (firstByte != 0x00L && firstByte != 0xFEL) {
                    throw IllegalStateException("The packet is not an OK packet, first byte: ${firstByte.toHexString()}")
                }

                val affectedRows = byteBuf.readLenEncInteger()
                val lastInsertId = byteBuf.readLenEncInteger()

                var statusFlags: EnumSet<ServerStatusFlag>? = null
                var warnings: Int? = null
                if (capabilities.contains(CapabilityFlag.CLIENT_PROTOCOL_41)) {
                    val flags = byteBuf.readFixedLengthInteger(2).toInt()
                    statusFlags = flags.toEnumSet()
                    warnings = byteBuf.readFixedLengthInteger(2).toInt()
                } else if (capabilities.contains(CapabilityFlag.CLIENT_TRANSACTIONS)) {
                    val flags = byteBuf.readFixedLengthInteger(2).toInt()
                    statusFlags = flags.toEnumSet()
                }

                var info: String? = null
                if (capabilities.contains(CapabilityFlag.CLIENT_SESSION_TRACK)) {
                    if (statusFlags?.contains(ServerStatusFlag.SERVER_SESSION_STATE_CHANGED) == true ||
                        (false /* TODO: handle status is not empty */)
                    ) {
                        info = byteBuf.readLenEncString().toString(Charsets.UTF_8)
                    }
                } else {
                    if (capabilities.contains(CapabilityFlag.CLIENT_SESSION_TRACK)) {
                        info = byteBuf.readRestOfPacketString().toString(Charsets.UTF_8)
                    }
                }

                return Ok(
                    affectedRows = affectedRows,
                    lastInsertId = lastInsertId,
                    statusFlags = statusFlags,
                    warnings = warnings,
                    info = info,
                )
            }
        }
    }
}
