package com.github.l34130.netty.dbgw.protocol.mysql.connection

import com.github.l34130.netty.dbgw.core.MessageAction
import com.github.l34130.netty.dbgw.protocol.mysql.MySqlGatewayState
import com.github.l34130.netty.dbgw.protocol.mysql.Packet
import io.netty.channel.ChannelHandlerContext

internal class AuthSwitchState(
    private val password: String?,
) : MySqlGatewayState() {
    private var req: AuthSwitchRequest? = null

    override fun onFrontendMessage(
        ctx: ChannelHandlerContext,
        msg: Packet,
    ): StateResult {
        val req = checkNotNull(req) { "AuthSwitchRequest not received yet, cannot process frontend message" }
        val resp = AuthSwitchResponse.readFrom(ctx, msg)
        return StateResult(
            nextState = AuthExchangeContinuationState(req.pluginName, password),
            action =
                if (password == null) {
                    MessageAction.Forward
                } else {
                    MessageAction.Transform(
                        newMsg =
                            resp.copy(
                                responseData =
                                    when (req.pluginName) {
                                        "mysql_native_password" -> MySqlNativePasswordEncoder.encode(req.pluginProvidedData, password)
                                        "caching_sha2_password" -> CachingSha256PasswordEncoder.encode(req.pluginProvidedData, password)
                                        else -> throw NotImplementedError("Unsupported authentication plugin: ${req.pluginName}")
                                    },
                            ),
                    )
                },
        )
    }

    override fun onBackendMessage(
        ctx: ChannelHandlerContext,
        msg: Packet,
    ): StateResult {
        check(req == null) { "AuthSwitchRequest already received, cannot process another packet" }
        this.req = AuthSwitchRequest.readFrom(ctx, msg)
        return StateResult(
            nextState = this,
            action = MessageAction.Forward,
        )
    }
}
