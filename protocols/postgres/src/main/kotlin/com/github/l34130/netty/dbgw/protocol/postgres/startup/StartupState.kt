package com.github.l34130.netty.dbgw.protocol.postgres.startup

import com.github.l34130.netty.dbgw.core.BusinessLogicAware
import com.github.l34130.netty.dbgw.core.GatewayState
import com.github.l34130.netty.dbgw.core.MessageAction
import com.github.l34130.netty.dbgw.core.audit
import com.github.l34130.netty.dbgw.core.audit.AuthenticationStartAuditEvent
import com.github.l34130.netty.dbgw.core.backend
import com.github.l34130.netty.dbgw.core.databaseCtx
import com.github.l34130.netty.dbgw.core.databasePolicyChain
import com.github.l34130.netty.dbgw.policy.api.PolicyDecision
import com.github.l34130.netty.dbgw.policy.api.database.DatabaseAuthenticationEvent
import com.github.l34130.netty.dbgw.policy.api.database.DatabaseAuthenticationPolicyContext.Companion.toAuthenticationPolicyContext
import com.github.l34130.netty.dbgw.policy.api.database.DatabasePolicyContext.Companion.toPolicyContext
import com.github.l34130.netty.dbgw.protocol.postgres.Message
import com.github.l34130.netty.dbgw.protocol.postgres.MessageConvertibleHandler
import com.github.l34130.netty.dbgw.protocol.postgres.MessageDecoder
import com.github.l34130.netty.dbgw.protocol.postgres.MessageEncoder
import com.github.l34130.netty.dbgw.protocol.postgres.constant.ErrorField
import com.github.l34130.netty.dbgw.protocol.postgres.message.ErrorResponse
import com.github.l34130.netty.dbgw.protocol.postgres.message.StartupMessage
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext

class StartupState :
    GatewayState<ByteBuf, Message>(),
    BusinessLogicAware {
    override fun onFrontendMessage(
        ctx: ChannelHandlerContext,
        msg: ByteBuf,
    ): StateResult {
        msg.markReaderIndex()
        val startupMsg = StartupMessage.readFrom(msg)
        logger.debug { "$startupMsg" }

        ctx.databaseCtx()!!.apply {
            connectionInfo.database = startupMsg.database
            connectionInfo.schema = startupMsg.replication
        }

        // Need to reset the reader index because this does not reset the reader index by MessageDecoder
        msg.resetReaderIndex()

        ctx.pipeline().also {
            it.addFirst(MessageConvertibleHandler())
            it.addFirst("encoder", MessageEncoder())
            it.addFirst("decoder", MessageDecoder())
        }
        ctx.backend().pipeline().also {
            it.addFirst(MessageConvertibleHandler())
            it.addFirst("decoder", MessageDecoder())
            // encoder will be added later to escape the startup message encoding; StartupMessage is not a message format
        }

        ctx.audit().emit(
            AuthenticationStartAuditEvent(
                ctx = ctx.databaseCtx()!!,
                evt =
                    DatabaseAuthenticationEvent(
                        username = startupMsg.user,
                    ),
            ),
        )

        val policyCtx = ctx.databaseCtx()!!.toPolicyContext().toAuthenticationPolicyContext(startupMsg.user)
        ctx.databasePolicyChain()!!.onAuthentication(policyCtx)
        policyCtx.decision.let { decision ->
            if (decision is PolicyDecision.Deny) {
                return StateResult(
                    nextState = this,
                    action =
                        MessageAction.Intercept(
                            ErrorResponse(
                                type = ErrorField.M,
                                value = "Access denied: ${decision.reason}",
                            ).asMessage(),
                        ),
                )
            }
        }

        return StateResult(
            nextState = AuthenticationState(policyCtx.username, policyCtx.password),
            action =
                MessageAction.Transform(
                    newMsg = startupMsg.copy(user = policyCtx.username).asByteBuf(),
                ),
        )
    }

    private val logger = KotlinLogging.logger { }
}
