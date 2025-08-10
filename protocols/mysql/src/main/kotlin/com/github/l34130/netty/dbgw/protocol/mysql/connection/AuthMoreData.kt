package com.github.l34130.netty.dbgw.protocol.mysql.connection

import com.github.l34130.netty.dbgw.core.util.netty.toByteArray
import com.github.l34130.netty.dbgw.protocol.mysql.Packet
import com.github.l34130.netty.dbgw.protocol.mysql.PacketConvertible
import com.github.l34130.netty.dbgw.protocol.mysql.readRestOfPacketString
import com.github.l34130.netty.dbgw.protocol.mysql.writeNullTerminatedString
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext

// https://dev.mysql.com/doc/dev/mysql-server/latest/page_protocol_connection_phase_packets_protocol_auth_more_data.html
internal class AuthMoreData(
    private val sequenceId: Int,
    val extraData: String,
) : PacketConvertible {
    override fun asPacket(): Packet {
        val payload =
            Unpooled.directBuffer().apply {
                writeByte(0x01)
                writeNullTerminatedString(extraData)
            }
        return Packet(sequenceId, payload)
    }

    companion object {
        fun readFrom(
            ctx: ChannelHandlerContext,
            msg: Packet,
        ): AuthMoreData {
            check(msg.payload.readByte().toInt() == 0x01) {
                "AuthMoreData packet must start with 0x01, but got ${msg.payload.readByte().toInt()}"
            }

            val extraData = msg.payload.readRestOfPacketString().toByteArray()
            return AuthMoreData(
                sequenceId = msg.sequenceId,
                extraData = extraData.toString(Charsets.UTF_8),
            )
        }
    }
}
