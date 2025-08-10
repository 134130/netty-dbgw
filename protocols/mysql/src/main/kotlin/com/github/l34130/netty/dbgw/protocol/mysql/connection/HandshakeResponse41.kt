package com.github.l34130.netty.dbgw.protocol.mysql.connection

import com.github.l34130.netty.dbgw.core.util.netty.toByteArray
import com.github.l34130.netty.dbgw.core.util.toEnumSet
import com.github.l34130.netty.dbgw.core.util.toFlags
import com.github.l34130.netty.dbgw.protocol.mysql.Packet
import com.github.l34130.netty.dbgw.protocol.mysql.PacketConvertible
import com.github.l34130.netty.dbgw.protocol.mysql.capabilities
import com.github.l34130.netty.dbgw.protocol.mysql.constant.CapabilityFlag
import com.github.l34130.netty.dbgw.protocol.mysql.readFixedLengthInteger
import com.github.l34130.netty.dbgw.protocol.mysql.readLenEncInteger
import com.github.l34130.netty.dbgw.protocol.mysql.readLenEncString
import com.github.l34130.netty.dbgw.protocol.mysql.readNullTerminatedString
import com.github.l34130.netty.dbgw.protocol.mysql.writeFixedLengthInteger
import com.github.l34130.netty.dbgw.protocol.mysql.writeLenEncInteger
import com.github.l34130.netty.dbgw.protocol.mysql.writeLenEncString
import com.github.l34130.netty.dbgw.protocol.mysql.writeNullTerminatedString
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import java.util.EnumSet

internal data class HandshakeResponse41(
    private val sequenceId: Int,
    val clientCapabilities: EnumSet<CapabilityFlag>,
    val maxPacketSize: ULong,
    val characterSet: Int,
    val username: String,
    val authResponse: ByteArray,
    private val authResponseLength: Int?, // null if CLIENT_PLUGIN_AUTH_LENENC_CLIENT_DATA is used
    val database: String?,
    val clientPluginName: String?,
    val clientConnectAttrs: Map<String, String>,
    val zstdCompressionLevel: Int,
) : PacketConvertible {
    override fun asPacket(): Packet {
        val payload = Unpooled.directBuffer()
        payload.writeFixedLengthInteger(4, clientCapabilities.toFlags())
        payload.writeFixedLengthInteger(4, maxPacketSize)
        payload.writeFixedLengthInteger(1, characterSet.toULong())

        payload.writeZero(23) // filler bytes

        payload.writeNullTerminatedString(username)

        authResponseLength?.let {
            payload.writeFixedLengthInteger(1, it.toULong())
        }
        payload.writeLenEncString(authResponse)

        database?.let { payload.writeNullTerminatedString(it) }
        clientPluginName?.let { payload.writeNullTerminatedString(it) }

        if (clientCapabilities.contains(CapabilityFlag.CLIENT_CONNECT_ATTRS)) {
            val attrsPayload = Unpooled.buffer()
            clientConnectAttrs.forEach { (key, value) ->
                attrsPayload.writeLenEncString(key)
                attrsPayload.writeLenEncString(value)
            }
            payload.writeLenEncInteger(attrsPayload.readableBytes().toULong())
            payload.writeBytes(attrsPayload)
        }

        if (clientCapabilities.contains(CapabilityFlag.CLIENT_ZSTD_COMPRESSION_ALGORITHM)) {
            payload.writeFixedLengthInteger(1, zstdCompressionLevel.toULong())
        }

        return Packet.of(sequenceId = sequenceId, payload = payload)
    }

    companion object {
        fun readFrom(
            ctx: ChannelHandlerContext,
            packet: Packet,
        ): HandshakeResponse41 {
            val msg = packet.payload
            val clientFlag = msg.readFixedLengthInteger(4)
            val clientCapabilities: EnumSet<CapabilityFlag> = clientFlag.toEnumSet()
            ctx.capabilities().setClientCapabilities(clientCapabilities)
            if (!clientCapabilities.contains(CapabilityFlag.CLIENT_PROTOCOL_41)) {
                ctx.close()
                throw IllegalStateException("Unsupported MySQL client protocol version: $clientFlag")
            }

            val maxPacketSize = msg.readFixedLengthInteger(4)
            val characterSet = msg.readFixedLengthInteger(1).toInt()
            when (characterSet) {
                1 -> "big5_chinese_ci"
                2 -> "latin2_czech_cs"
                3 -> "dec8_swedish_ci"
                4 -> "cp850_general_ci"
                5 -> "latin1_german1_ci"
                6 -> "hp8_english_ci"
                7 -> "koi8_ru_general_ci"
                8 -> "latin1_swedish_ci"
                9 -> "latin2_general_ci"
                10 -> "swe7_swedish_ci"
                33 -> "utf8mb3_general_ci"
                63 -> "binary"
                else -> "unknown_character_set($characterSet)"
            }
            msg.skipBytes(23) // skip filler bytes

            // login username
            val username = msg.readNullTerminatedString().toString(Charsets.UTF_8)

            val (authResponse, authResponseLength) =
                if (clientCapabilities.contains(CapabilityFlag.CLIENT_PLUGIN_AUTH_LENENC_CLIENT_DATA)) {
                    msg.readLenEncString() to null
                } else {
                    val authResponseLength = msg.readFixedLengthInteger(1).toInt()
                    msg.readLenEncString() to authResponseLength
                }

            val database =
                if (clientCapabilities.contains(CapabilityFlag.CLIENT_CONNECT_WITH_DB)) {
                    msg.readNullTerminatedString().toString(Charsets.UTF_8)
                } else {
                    null
                }

            val clientPluginName =
                if (clientCapabilities.contains(CapabilityFlag.CLIENT_PLUGIN_AUTH)) {
                    msg.readNullTerminatedString()
                } else {
                    null
                }?.toString(Charsets.UTF_8)

            val clientConnectAttrs: Map<String, String> =
                if (clientCapabilities.contains(CapabilityFlag.CLIENT_CONNECT_ATTRS)) {
                    val lengthOfAllKeyValues = msg.readLenEncInteger()

                    val keyValuesByteBuf = msg.readSlice(lengthOfAllKeyValues.toInt())

                    val attrs = mutableMapOf<String, String>()
                    while (keyValuesByteBuf.readableBytes() > 0) {
                        val key = keyValuesByteBuf.readLenEncString().toString(Charsets.UTF_8)
                        val value = keyValuesByteBuf.readLenEncString().toString(Charsets.UTF_8)
                        attrs.put(key, value)
                    }
                    attrs
                } else {
                    emptyMap()
                }

            val zstdCompressionLevel =
                if (clientCapabilities.contains(CapabilityFlag.CLIENT_ZSTD_COMPRESSION_ALGORITHM)) {
                    msg.readFixedLengthInteger(1).toInt()
                } else {
                    0
                }

            return HandshakeResponse41(
                sequenceId = packet.sequenceId,
                clientCapabilities = clientCapabilities,
                maxPacketSize = maxPacketSize,
                characterSet = characterSet,
                username = username,
                authResponse = authResponse.toByteArray(),
                authResponseLength = authResponseLength,
                database = database,
                clientPluginName = clientPluginName,
                clientConnectAttrs = clientConnectAttrs,
                zstdCompressionLevel = zstdCompressionLevel,
            )
        }
    }
}
