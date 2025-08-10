package com.github.l34130.netty.dbgw.protocol.mysql.connection

import com.github.l34130.netty.dbgw.core.MessageAction
import com.github.l34130.netty.dbgw.core.util.netty.peek
import com.github.l34130.netty.dbgw.protocol.mysql.MySqlGatewayState
import com.github.l34130.netty.dbgw.protocol.mysql.Packet
import com.github.l34130.netty.dbgw.protocol.mysql.capabilities
import com.github.l34130.netty.dbgw.protocol.mysql.command.CommandPhaseState
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.channel.ChannelHandlerContext

internal class AuthExchangeContinuationState(
    private val pluginName: String,
    private val password: String?,
) : MySqlGatewayState() {
    override fun onFrontendMessage(
        ctx: ChannelHandlerContext,
        msg: Packet,
    ): StateResult {
        val resp = AuthSwitchResponse.readFrom(ctx, msg)

        if (password == null) {
            // If no password is provided, we just forward the response
            return StateResult(
                nextState = AuthResultState(pluginName, password),
                action = MessageAction.Forward,
            )
        }

        return StateResult(
            nextState = AuthResultState(pluginName, password),
            action =
                MessageAction.Transform(
                    newMsg =
                        resp.copy(
                            responseData =
                                when (pluginName) {
                                    "mysql_native_password" -> MySqlNativePasswordEncoder.encode(resp.responseData, password)
                                    "caching_sha2_password" -> CachingSha256PasswordEncoder.encode(resp.responseData, password)
                                    else -> throw NotImplementedError("Unsupported plugin: $pluginName")
                                },
                        ),
                ),
        )
    }

    override fun onBackendMessage(
        ctx: ChannelHandlerContext,
        msg: Packet,
    ): StateResult {
        val payload = msg.payload
        if (payload.peek { it.readUnsignedByte().toUInt() } == 0x01u) {
            val msg = AuthMoreData.readFrom(ctx, msg)
            // Extra authentication data beyond the initial challenge
            return StateResult(
                nextState = this,
                action = MessageAction.Forward,
            )
        }

        if (msg.isOkPacket()) {
            logger.trace { "Authentication succeeded" }
            return StateResult(
                nextState = CommandPhaseState(),
                action = MessageAction.Forward,
            )
        }

        if (msg.isErrorPacket()) {
            logger.trace {
                "Authentication failed: ${Packet.Error.readFrom(msg.payload, ctx.capabilities().enumSet())}"
            }
            return StateResult(
                nextState = this,
                action = MessageAction.Forward,
            )
        }

        throw IllegalStateException("Received unexpected packet type during authentication")
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}
