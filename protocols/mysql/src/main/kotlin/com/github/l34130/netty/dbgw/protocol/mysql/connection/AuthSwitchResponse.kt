package com.github.l34130.netty.dbgw.protocol.mysql.connection

import com.github.l34130.netty.dbgw.protocol.mysql.Packet
import com.github.l34130.netty.dbgw.protocol.mysql.PacketConvertible
import com.github.l34130.netty.dbgw.protocol.mysql.readRestOfPacketString
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext

internal data class AuthSwitchResponse(
    private val sequenceId: Int,
    val responseData: ByteArray,
) : PacketConvertible {
    override fun asPacket(): Packet {
        val payload = Unpooled.copiedBuffer(responseData)
        return Packet(sequenceId, payload)
    }

    companion object {
        fun readFrom(
            ctx: ChannelHandlerContext,
            msg: Packet,
        ): AuthSwitchResponse {
            val responseData = msg.payload.readRestOfPacketString()
            return AuthSwitchResponse(
                sequenceId = msg.sequenceId,
                responseData = ByteBufUtil.getBytes(responseData),
            )
        }
    }
}
