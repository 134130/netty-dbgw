package com.github.l34130.netty.dbgw.protocol.postgres.startup

import com.github.l34130.netty.dbgw.core.GatewayState
import com.github.l34130.netty.dbgw.core.MessageAction
import com.github.l34130.netty.dbgw.protocol.postgres.Message
import com.github.l34130.netty.dbgw.protocol.postgres.MessageEncoder
import com.github.l34130.netty.dbgw.protocol.postgres.message.ErrorResponse
import com.github.l34130.netty.dbgw.protocol.postgres.message.ParameterStatusMessage
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.channel.ChannelHandlerContext

class AuthenticationState : GatewayState<Message, Message>() {
    private var authenticationRequest: AuthenticationRequest? = null

    override fun onFrontendMessage(
        ctx: ChannelHandlerContext,
        msg: Message,
    ): StateResult {
        val authReq = authenticationRequest
        checkNotNull(authReq) {
            "Authentication request has not been received yet. This state should only be used after an authentication request."
        }

        return when (authReq) {
            is AuthenticationRequest.AuthenticationSASL -> {
                val initialResp = SASLInitialResponse.readFrom(msg)
                logger.trace { "SASL initial response: $initialResp" }
                StateResult(
                    nextState = this,
                    action = MessageAction.Forward,
                )
            }
            is AuthenticationRequest.AuthenticationSASLContinue -> {
                val resp = SASLResponse.readFrom(msg)
                logger.trace { "SASL response: $resp" }
                StateResult(
                    nextState = this,
                    action = MessageAction.Forward,
                )
            }
            is AuthenticationRequest.AuthenticationSASLFinal ->
                error("Never should reach here, final response should be handled in onBackendMessage")
            is AuthenticationRequest.AuthenticationMD5Password -> {
                val passwordMsg = PasswordMessage.readFrom(msg)
                logger.trace { "MD5 password response: $passwordMsg" }
                StateResult(
                    nextState = this,
                    action = MessageAction.Forward,
                )
            }
            AuthenticationRequest.AuthenticationOk -> error("Never should reach here, ok response should be handled in onBackendMessage")
        }
    }

    override fun onBackendMessage(
        ctx: ChannelHandlerContext,
        msg: Message,
    ): StateResult {
        ctx.pipeline().also {
            if (it.get("encoder") == null) {
                it.addFirst("encoder", MessageEncoder())
            }
        }

        return when (msg.type) {
            'R' -> {
                authenticationRequest = AuthenticationRequest.readFrom(msg)

                when (authenticationRequest) {
                    is AuthenticationRequest.AuthenticationOk -> {
                        logger.trace { "Authentication successful: $authenticationRequest" }
                        StateResult(
                            nextState = AuthenticationResultState(),
                            action = MessageAction.Forward,
                        )
                    }
                    is AuthenticationRequest.AuthenticationSASLFinal -> {
                        logger.trace { "Final SASL authentication request: $authenticationRequest" }
                        StateResult(
                            nextState = AuthenticationResultState(),
                            action = MessageAction.Forward,
                        )
                    }
                    else -> {
                        logger.trace { "Authentication request: $authenticationRequest" }
                        StateResult(
                            nextState = this,
                            action = MessageAction.Forward,
                        )
                    }
                }
            }
            'S' -> {
                val paramStatusMsg = ParameterStatusMessage.readFrom(msg)
                logger.trace { "Parameter: ${paramStatusMsg.parameterName} = ${paramStatusMsg.parameterValue}" }
                StateResult(
                    nextState = this,
                    action = MessageAction.Forward,
                )
            }
            'E' -> {
                val errorResp = ErrorResponse.readFrom(msg)
                logger.trace { "Error response: $errorResp" }
                StateResult(
                    nextState = this,
                    action = MessageAction.Forward,
                )
            }
            else -> TODO("Unsupported message type: ${msg.type}")
        }
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}
