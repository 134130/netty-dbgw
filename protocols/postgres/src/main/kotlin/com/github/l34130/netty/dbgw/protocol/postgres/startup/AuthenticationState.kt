package com.github.l34130.netty.dbgw.protocol.postgres.startup

import com.github.l34130.netty.dbgw.core.GatewayState
import com.github.l34130.netty.dbgw.core.MessageAction
import com.github.l34130.netty.dbgw.protocol.postgres.Message
import com.github.l34130.netty.dbgw.protocol.postgres.MessageEncoder
import com.github.l34130.netty.dbgw.protocol.postgres.SaslUtils
import com.github.l34130.netty.dbgw.protocol.postgres.message.ErrorResponse
import com.github.l34130.netty.dbgw.protocol.postgres.message.ParameterStatusMessage
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.channel.ChannelHandlerContext
import java.util.Base64

class AuthenticationState(
    val user: String,
    val password: String?,
) : GatewayState<Message, Message>() {
    private var lastAuthenticationRequest: AuthenticationRequest? = null

    private var saslInitialResponse: SASLInitialResponse? = null
    private var saslContinueRequest: AuthenticationRequest.AuthenticationSASLContinue? = null

    override fun onFrontendMessage(
        ctx: ChannelHandlerContext,
        msg: Message,
    ): StateResult {
        val lastAuthReq = lastAuthenticationRequest
        checkNotNull(lastAuthReq) {
            "Authentication request has not been received yet. This state should only be used after an authentication request."
        }

        return when (lastAuthReq) {
            is AuthenticationRequest.AuthenticationMD5Password -> {
                val passwordMsg = PasswordMessage.readFrom(msg)

                val password = password
                if (password != null) {
                    StateResult(
                        nextState = this,
                        action =
                            MessageAction.Transform(
                                newMsg = PasswordMessage.ofMd5(user, password, lastAuthReq.salt),
                            ),
                    )
                } else {
                    StateResult(
                        nextState = this,
                        action = MessageAction.Forward,
                    )
                }
            }
            is AuthenticationRequest.AuthenticationSASL -> {
                val initialResp =
                    SASLInitialResponse.readFrom(msg).also {
                        saslInitialResponse = it
                    }

                when (initialResp.mechanism) {
                    "SCRAM-SHA-256" -> {
                        StateResult(
                            nextState = this,
                            action =
                                MessageAction.Transform(
                                    newMsg =
                                        initialResp.copy(),
                                ),
                        )
                    }
                    else -> {
                        // Unsupported SASL mechanism
                        StateResult(
                            nextState = this,
                            action =
                                MessageAction.Intercept(
                                    ErrorResponse.of(
                                        severity = "FATAL",
                                        code = "28000",
                                        message = "Unsupported SASL mechanism: ${initialResp.mechanism}",
                                    ),
                                ),
                        )
                    }
                }
            }
            is AuthenticationRequest.AuthenticationSASLContinue -> {
                val initResp =
                    checkNotNull(saslInitialResponse) {
                        "SASL initial response has not been set. This state should only be used after a SASL initial response."
                    }
                val continueReq =
                    checkNotNull(saslContinueRequest) {
                        "SASL continue request has not been set. This state should only be used after a SASL continue request."
                    }

                if (password == null) {
                    return StateResult(
                        nextState = this,
                        action = MessageAction.Forward, // No password provided, just forward the message
                    )
                }

                val clientProof =
                    SaslUtils.generateScramClientProof(
                        password = password,
                        initialResponseAttrs = initResp.attributes,
                        continueRequestAttrs = continueReq.attributes,
                    )

                val resp = SASLResponse.readFrom(msg)

                StateResult(
                    nextState = this,
                    action =
                        MessageAction.Transform(
                            SASLResponse(
                                mapOf(
                                    "c" to "biws",
                                    "r" to lastAuthReq.attributes["r"]!!,
                                    "p" to Base64.getEncoder().encodeToString(clientProof.first),
                                ),
                            ),
                        ),
                )
            }
            AuthenticationRequest.AuthenticationOk -> error("Never should reach here, ok response should be handled in onBackendMessage")
            else -> error("Unexpected authentication request type: $lastAuthReq")
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
                lastAuthenticationRequest = AuthenticationRequest.readFrom(msg)

                when (val req = lastAuthenticationRequest) {
                    is AuthenticationRequest.AuthenticationSASL ->
                        StateResult(
                            nextState = this,
                            action = MessageAction.Forward,
                        )
                    is AuthenticationRequest.AuthenticationSASLContinue -> {
                        saslContinueRequest = req
                        StateResult(
                            nextState = this,
                            action = MessageAction.Forward,
                        )
                    }
                    is AuthenticationRequest.AuthenticationSASLFinal -> {
                        val initResp =
                            checkNotNull(saslInitialResponse) {
                                "SASL initial response has not been set. This state should only be used after a SASL initial response."
                            }
                        val continueReq =
                            checkNotNull(saslContinueRequest) {
                                "SASL continue request has not been set. This state should only be used after a SASL continue request."
                            }

                        // If password is null, just passthrough the message
                        if (password == null) {
                            return StateResult(
                                nextState = this,
                                action = MessageAction.Forward,
                            )
                        }

//                        val clientProof =
//                            SaslUtils.generateScramClientProof(
//                                password = "password",
//                                initialResponseAttrs = initResp.attributes,
//                                continueRequestAttrs = continueReq.attributes,
//                            )

                        // Currently, We just drop the server signature here.
                        // TODO: Handle if password checking is required.
                        return StateResult(
                            nextState = AuthenticationResultState(),
                            action = MessageAction.Drop,
                        )
                    }
                    is AuthenticationRequest.AuthenticationOk -> {
                        logger.trace { "Authentication successful: $req" }
                        StateResult(
                            nextState = AuthenticationResultState(),
                            action = MessageAction.Forward,
                        )
                    }
                    is AuthenticationRequest.AuthenticationMD5Password -> {
                        logger.trace { "MD5 password authentication request: $req" }
                        StateResult(
                            nextState = this,
                            action = MessageAction.Forward,
                        )
                    }
                    else -> error("Unsupported authentication request type: $req")
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
