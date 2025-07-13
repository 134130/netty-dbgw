@file:OptIn(ExperimentalStdlibApi::class)

package com.github.l34130.netty.dbgw.app.handler.codec.mysql

import com.github.l34130.netty.dbgw.app.handler.codec.mysql.data.FixedLengthInteger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder
import io.netty.handler.codec.MessageToByteEncoder
import java.util.*

class Packet(
    val payloadLength: Int,
    val sequenceId: Int,
    val payload: ByteBuf,
) {
    override fun toString(): String =
        "Packet(payloadLength=$payloadLength, sequenceId=$sequenceId, payload=${payload.readableBytes()} bytes)"

    fun isErrorPacket(): Boolean {
        val firstByte = payload.peek { it.readFixedLengthInteger(1) }?.value
        return firstByte == 0xFFL
    }

    fun isOkPacket(): Boolean {
        val firstByte = payload.peek { it.readFixedLengthInteger(1) }?.value
        return firstByte == 0x00L ||
            firstByte == 0xFEL // 0xFE is used for OK packets with warnings
    }

    // https://dev.mysql.com/doc/dev/mysql-server/latest/page_protocol_basic_eof_packet.html
    data class Eof(
        val warnings: Int?,
        val statusFlags: Int?,
    ) {
        override fun toString(): String = "Packet.Eof(warnings=$warnings, statusFlags=$statusFlags)"

        companion object {
            fun readFrom(
                byteBuf: ByteBuf,
                clientCapabilities: EnumSet<CapabilityFlag>,
            ): Eof {
                val firstByte = byteBuf.readFixedLengthInteger(1).value
                if (firstByte != 0xFEL) {
                    throw IllegalStateException("The packet is not an EOF packet, first byte: ${firstByte.toHexString()}")
                }

                var warnings: Int? = null
                var statusFlags: Int? = null
                if (clientCapabilities.contains(CapabilityFlag.CLIENT_PROTOCOL_41)) {
                    warnings = byteBuf.readFixedLengthInteger(2).value.toInt()
                    statusFlags = byteBuf.readFixedLengthInteger(2).value.toInt()
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
                clientCapabilities: EnumSet<CapabilityFlag>,
            ): Error {
                val firstByte = byteBuf.readFixedLengthInteger(1).value
                if (firstByte != 0xFFL) {
                    throw IllegalStateException("The packet is not an error packet, first byte: ${firstByte.toHexString()}")
                }

                val errorCode = byteBuf.readFixedLengthInteger(2).value.toInt()
                var sqlStateMarker: String? = null
                var sqlState: String? = null
                if (clientCapabilities.contains(CapabilityFlag.CLIENT_PROTOCOL_41)) {
                    sqlStateMarker = byteBuf.readFixedLengthString(1)
                    sqlState = byteBuf.readFixedLengthString(5)
                }
                val message = byteBuf.readNullTerminatedString().toString(Charsets.UTF_8)

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
                clientCapabilities: EnumSet<CapabilityFlag>,
            ): Ok {
                val firstByte = byteBuf.readFixedLengthInteger(1).value
                if (firstByte != 0x00L && firstByte != 0xFEL) {
                    throw IllegalStateException("The packet is not an OK packet, first byte: ${firstByte.toHexString()}")
                }

                val affectedRows = byteBuf.readLenEncInteger()
                val lastInsertId = byteBuf.readLenEncInteger()

                var statusFlags: EnumSet<ServerStatusFlag>? = null
                var warnings: Int? = null
                if (clientCapabilities.contains(CapabilityFlag.CLIENT_PROTOCOL_41)) {
                    val flags = byteBuf.readFixedLengthInteger(2).value.toInt()
                    statusFlags = flags.toEnumSet()
                    warnings = byteBuf.readFixedLengthInteger(2).value.toInt()
                } else if (clientCapabilities.contains(CapabilityFlag.CLIENT_TRANSACTIONS)) {
                    val flags = byteBuf.readFixedLengthInteger(2).value.toInt()
                    statusFlags = flags.toEnumSet()
                }

                var info: String? = null
                if (clientCapabilities.contains(CapabilityFlag.CLIENT_SESSION_TRACK)) {
                    if (statusFlags?.contains(ServerStatusFlag.SERVER_SESSION_STATE_CHANGED) == true ||
                        (false /* TODO: handle status is not empty */)
                    ) {
                        info = byteBuf.readLenEncString().toString(Charsets.UTF_8)
                    }
                } else {
                    if (clientCapabilities.contains(CapabilityFlag.CLIENT_SESSION_TRACK)) {
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

class PacketDecoder : ByteToMessageDecoder() {
    override fun decode(
        ctx: ChannelHandlerContext,
        `in`: ByteBuf,
        out: MutableList<Any?>,
    ) {
        if (`in`.readableBytes() < HEADER_SIZE) {
            // Not enough bytes to read a complete packet header.
            return
        }

        `in`.markReaderIndex()

        val payloadLength = `in`.readFixedLengthInteger(3)
        val sequenceId = `in`.readFixedLengthInteger(1)

        if (`in`.readableBytes() < payloadLength.value) {
            // Not enough bytes to read the complete payload.
            `in`.resetReaderIndex()
            return
        }

        val payload = `in`.readSlice(payloadLength.value.toInt())
        out +=
            Packet(
                payloadLength = payloadLength.value.toInt(),
                sequenceId = sequenceId.value.toInt(),
                payload = payload.retain(),
            )
    }

    companion object {
        const val HEADER_SIZE = 4
    }
}

class PacketEncoder : MessageToByteEncoder<Packet>() {
    override fun encode(
        ctx: ChannelHandlerContext,
        msg: Packet,
        out: ByteBuf,
    ) {
        if (msg.payload.readerIndex() != 0) {
            logger.warn {
                "Packet payload reader index is not at 0, resetting it. " +
                    "This may lead to unexpected behavior if the payload is not fully read."
            }
            msg.payload.readerIndex(0)
        }

        FixedLengthInteger(3, msg.payload.readableBytes()).writeTo(out)
        FixedLengthInteger(1, msg.sequenceId).writeTo(out)
        out.writeBytes(msg.payload.nioBuffer())
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}
