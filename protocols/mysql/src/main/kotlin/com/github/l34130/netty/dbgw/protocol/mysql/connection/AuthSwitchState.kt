package com.github.l34130.netty.dbgw.protocol.mysql.connection

import com.github.l34130.netty.dbgw.core.MessageAction
import com.github.l34130.netty.dbgw.protocol.mysql.MySqlGatewayState
import com.github.l34130.netty.dbgw.protocol.mysql.Packet
import com.github.l34130.netty.dbgw.protocol.mysql.readNullTerminatedString
import com.github.l34130.netty.dbgw.protocol.mysql.readRestOfPacketString
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.channel.ChannelHandlerContext

internal class AuthSwitchState : MySqlGatewayState() {
    private var state: State = State.WAITING_REQUEST

    override fun onDownstreamMessage(
        ctx: ChannelHandlerContext,
        msg: Packet,
    ): StateResult =
        when (state) {
            State.WAITING_REQUEST -> error("Unexpected downstream packet in $state state")
            State.WAITING_RESPONSE -> handleAuthSwitchResponse(ctx, msg)
        }

    override fun onUpstreamMessage(
        ctx: ChannelHandlerContext,
        msg: Packet,
    ): StateResult =
        when (state) {
            State.WAITING_REQUEST -> handleAuthSwitchRequest(ctx, msg)
            State.WAITING_RESPONSE -> error("Unexpected upstream packet in $state state")
        }

    private fun handleAuthSwitchRequest(
        ctx: ChannelHandlerContext,
        packet: Packet,
    ): StateResult {
        val payload = packet.payload

        val firstByte = payload.readUnsignedByte().toUInt()
        if (firstByte != 0xFE.toUInt()) {
            throw IllegalStateException("Expected AuthSwitchRequest packet, but received: ${"%02x".format(firstByte).uppercase()}")
        }

        val pluginName = payload.readNullTerminatedString().toString(Charsets.US_ASCII)
        logger.trace { "Received AuthSwitchRequest with plugin: $pluginName" }
        val pluginProvidedData = payload.readRestOfPacketString().toString(Charsets.UTF_8)

        state = State.WAITING_RESPONSE
        return StateResult(
            nextState = this,
            action = MessageAction.Forward,
        )
    }

    private fun handleAuthSwitchResponse(
        ctx: ChannelHandlerContext,
        packet: Packet,
    ): StateResult {
        val payload = packet.payload
        if (payload.readableBytes() < 1) {
            throw IllegalStateException("Received AuthSwitchResponse with no data")
        }

        val responseData = payload.readRestOfPacketString().toString(Charsets.UTF_8)
        logger.trace { "Received AuthSwitchResponse with data: $responseData" }

        return StateResult(
            nextState = AuthExchangeContinuationState(),
            action = MessageAction.Forward,
        )
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    private enum class State {
        WAITING_REQUEST,
        WAITING_RESPONSE,
    }
}
