package com.github.l34130.netty.dbgw.protocol.mysql.connection

import com.github.l34130.netty.dbgw.core.downstream
import com.github.l34130.netty.dbgw.core.upstream
import com.github.l34130.netty.dbgw.protocol.mysql.MySqlGatewayState
import com.github.l34130.netty.dbgw.protocol.mysql.Packet
import com.github.l34130.netty.dbgw.protocol.mysql.readNullTerminatedString
import com.github.l34130.netty.dbgw.protocol.mysql.readRestOfPacketString
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.channel.ChannelHandlerContext

internal class AuthSwitchState : MySqlGatewayState {
    private var state: State = State.WAITING_REQUEST

    override fun onDownstreamMessage(
        ctx: ChannelHandlerContext,
        msg: Packet,
    ): MySqlGatewayState =
        when (state) {
            State.WAITING_REQUEST -> error("Unexpected downstream packet in $state state")
            State.WAITING_RESPONSE -> handleAuthSwitchResponse(ctx, msg)
        }

    override fun onUpstreamMessage(
        ctx: ChannelHandlerContext,
        msg: Packet,
    ): MySqlGatewayState =
        when (state) {
            State.WAITING_REQUEST -> handleAuthSwitchRequest(ctx, msg)
            State.WAITING_RESPONSE -> error("Unexpected upstream packet in $state state")
        }

    private fun handleAuthSwitchRequest(
        ctx: ChannelHandlerContext,
        packet: Packet,
    ): MySqlGatewayState {
        val payload = packet.payload
        payload.markReaderIndex()

        val firstByte = payload.readUnsignedByte().toUInt()
        if (firstByte != 0xFE.toUInt()) {
            throw IllegalStateException("Expected AuthSwitchRequest packet, but received: ${"%02x".format(firstByte).uppercase()}")
        }

        val pluginName = payload.readNullTerminatedString().toString(Charsets.US_ASCII)
        logger.trace { "Received AuthSwitchRequest with plugin: $pluginName" }
        val pluginProvidedData = payload.readRestOfPacketString().toString(Charsets.UTF_8)

        payload.resetReaderIndex()
        ctx.downstream().writeAndFlush(packet)

        state = State.WAITING_RESPONSE
        return this
    }

    private fun handleAuthSwitchResponse(
        ctx: ChannelHandlerContext,
        packet: Packet,
    ): MySqlGatewayState {
        val payload = packet.payload
        payload.markReaderIndex()

        if (payload.readableBytes() < 1) {
            throw IllegalStateException("Received AuthSwitchResponse with no data")
        }

        val responseData = payload.readRestOfPacketString().toString(Charsets.UTF_8)
        logger.trace { "Received AuthSwitchResponse with data: $responseData" }

        payload.resetReaderIndex()
        ctx.upstream().writeAndFlush(packet)

        return AuthExchangeContinuationState()
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    private enum class State {
        WAITING_REQUEST,
        WAITING_RESPONSE,
    }
}
