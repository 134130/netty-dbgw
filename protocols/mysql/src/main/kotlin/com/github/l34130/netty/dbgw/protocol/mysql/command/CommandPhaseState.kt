package com.github.l34130.netty.dbgw.protocol.mysql.command

import com.github.l34130.netty.dbgw.core.BusinessLogicAware
import com.github.l34130.netty.dbgw.core.MessageAction
import com.github.l34130.netty.dbgw.core.audit
import com.github.l34130.netty.dbgw.core.audit.QueryStartAuditEvent
import com.github.l34130.netty.dbgw.core.databaseCtx
import com.github.l34130.netty.dbgw.core.databasePolicyChain
import com.github.l34130.netty.dbgw.core.utils.netty.peek
import com.github.l34130.netty.dbgw.policy.api.PolicyDecision
import com.github.l34130.netty.dbgw.policy.api.database.DatabasePolicyContext.Companion.toPolicyContext
import com.github.l34130.netty.dbgw.policy.api.database.DatabaseQueryEvent
import com.github.l34130.netty.dbgw.policy.api.database.DatabaseQueryPolicyContext.Companion.toQueryPolicyContext
import com.github.l34130.netty.dbgw.protocol.mysql.MySqlGatewayState
import com.github.l34130.netty.dbgw.protocol.mysql.Packet
import com.github.l34130.netty.dbgw.protocol.mysql.capabilities
import com.github.l34130.netty.dbgw.protocol.mysql.constant.CapabilityFlag
import com.github.l34130.netty.dbgw.protocol.mysql.constant.MySqlFieldType
import com.github.l34130.netty.dbgw.protocol.mysql.readFixedLengthInteger
import com.github.l34130.netty.dbgw.protocol.mysql.readLenEncInteger
import com.github.l34130.netty.dbgw.protocol.mysql.readLenEncString
import com.github.l34130.netty.dbgw.protocol.mysql.readRestOfPacketString
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.channel.ChannelHandlerContext

internal class CommandPhaseState :
    MySqlGatewayState(),
    BusinessLogicAware {
    override fun onFrontendMessage(
        ctx: ChannelHandlerContext,
        msg: Packet,
    ): StateResult {
        val payload = msg.payload
        val commandByte = payload.peek { it.readUnsignedByte().toUInt() } ?: error("Command byte is missing in the packet")
        val commandType = CommandType.from(commandByte)
        logger.debug { "Received $commandType" }
        return when (commandType) {
            CommandType.COM_QUERY -> handleQueryCommand(ctx, msg)
            CommandType.COM_PING -> PingCommandState().onFrontendMessage(ctx, msg)
            CommandType.COM_QUIT -> QuitCommandState().onFrontendMessage(ctx, msg)
            CommandType.COM_DEBUG -> DebugCommandState().onFrontendMessage(ctx, msg)
            CommandType.COM_STMT_PREPARE -> PrepareStatementCommandState().onFrontendMessage(ctx, msg)
            CommandType.COM_STMT_EXECUTE -> ExecuteStatementCommandState().onFrontendMessage(ctx, msg)
            CommandType.COM_STMT_CLOSE -> CloseStatementCommandState().onFrontendMessage(ctx, msg)
            null -> throw IllegalArgumentException("Unknown command byte: 0x${"%02x".format(commandByte).uppercase()}")
        }
    }

    private fun handleQueryCommand(
        ctx: ChannelHandlerContext,
        packet: Packet,
    ): StateResult {
        val payload = packet.payload
        payload.skipBytes(1) // skip command byte

        var parameters: List<Triple<MySqlFieldType, String, Any?>>? = null
        if (ctx.capabilities().contains(CapabilityFlag.CLIENT_QUERY_ATTRIBUTES)) {
            val parameterCount = payload.readLenEncInteger().toInt()
            val parameterSetCount = payload.readLenEncInteger().toInt() // always 1 currently
            logger.trace { "COM_QUERY: parameterCount=$parameterCount, parameterSetCount=$parameterSetCount" }
            if (parameterCount > 0) {
                val nullBitmap = payload.readString((parameterCount + 7) / 8, Charsets.UTF_8)
                val nextParamsBindFlag = payload.readFixedLengthInteger(1)
                if (nextParamsBindFlag != 1UL) {
                    // malformed packet, unexpected nextParamsBindFlag
                    logger.warn { "Unexpected nextParamsBindFlag: $nextParamsBindFlag" }
                }
                parameters = mutableListOf()
                (0 until parameterCount).forEach { i ->
                    val parameterTypeAndFlag = payload.readFixedLengthInteger(2)
                    val type = MySqlFieldType.of(parameterTypeAndFlag.toInt())
                    val parameterName = payload.readLenEncString()
                    parameters.add(Triple(type, parameterName.toString(Charsets.UTF_8), null))
                }
                logger.trace { "COM_QUERY: parameters=$parameters" }
            }
        }

        val query = payload.readRestOfPacketString().toString(Charsets.UTF_8)
        logger.debug { "COM_QUERY: query='$query'" }

        ctx.audit().emit(QueryStartAuditEvent(ctx.databaseCtx()!!, DatabaseQueryEvent(query)))

        ctx.databasePolicyChain()?.let { chain ->
            val policyCtx = ctx.databaseCtx()!!.toPolicyContext().toQueryPolicyContext(query)
            chain.onQuery(policyCtx)
            policyCtx.decision.let { decision ->
                if (decision is PolicyDecision.Deny) {
                    val errorPacket =
                        Packet.Error.of(
                            sequenceId = packet.sequenceId + 1,
                            errorCode = 1U,
                            sqlState = "DBGW_",
                            message =
                                buildString {
                                    append("Access denied")
                                    if (!decision.reason.isNullOrBlank()) {
                                        append(": ${decision.reason}")
                                    }
                                },
                            capabilities = ctx.capabilities().enumSet(),
                        )
                    return StateResult(
                        nextState = CommandPhaseState(),
                        action = MessageAction.Intercept(msg = errorPacket),
                    )
                }
            }
        }

        return StateResult(
            nextState = QueryCommandResponseState(),
            action = MessageAction.Forward,
        )
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    enum class CommandType(
        val code: UInt,
    ) {
        COM_QUERY(0x03u),
        COM_PING(0x0Eu),
        COM_QUIT(0x01u),
        COM_DEBUG(0x0Du),
        COM_STMT_PREPARE(0x16u),
        COM_STMT_EXECUTE(0x17u),
        COM_STMT_CLOSE(0x19u),
        ;

        companion object {
            private val map = entries.associateBy(CommandType::code)

            fun from(code: UInt): CommandType? = map[code]
        }
    }
}
