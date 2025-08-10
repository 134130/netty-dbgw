package com.github.l34130.netty.dbgw.protocol.mysql.connection

import com.github.l34130.netty.dbgw.common.util.toHexString
import com.github.l34130.netty.dbgw.core.util.netty.toByteArray
import com.github.l34130.netty.dbgw.protocol.mysql.Packet
import com.github.l34130.netty.dbgw.protocol.mysql.PacketConvertible
import com.github.l34130.netty.dbgw.protocol.mysql.readNullTerminatedString
import com.github.l34130.netty.dbgw.protocol.mysql.readRestOfPacketString
import com.github.l34130.netty.dbgw.protocol.mysql.writeNullTerminatedString
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext

internal class AuthSwitchRequest(
    private val sequenceId: Int,
    val pluginName: String,
    val pluginProvidedData: ByteArray,
) : PacketConvertible {
    override fun asPacket(): Packet {
        val payload =
            Unpooled.directBuffer().apply {
                writeByte(0xFE) // First byte for AuthSwitchRequest
                writeNullTerminatedString(pluginName)
                writeBytes(pluginProvidedData)
            }

        return Packet(sequenceId, payload)
    }

    companion object {
        fun readFrom(
            ctx: ChannelHandlerContext,
            msg: Packet,
        ): AuthSwitchRequest {
            val payload = msg.payload
            val firstByte = payload.readUnsignedByte().toInt()
            check(firstByte == 0xFE) {
                "Expected AuthSwitchRequest packet, but received: ${firstByte.toHexString()}"
            }

            val pluginName = payload.readNullTerminatedString().toString(Charsets.UTF_8)
            val pluginProvidedData = payload.readRestOfPacketString().toByteArray()

            return AuthSwitchRequest(
                sequenceId = msg.sequenceId,
                pluginName = pluginName,
                pluginProvidedData = pluginProvidedData,
            )
        }
    }
}
