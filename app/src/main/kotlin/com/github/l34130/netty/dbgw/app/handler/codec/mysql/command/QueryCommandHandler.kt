package com.github.l34130.netty.dbgw.app.handler.codec.mysql.command

import com.github.l34130.netty.dbgw.app.handler.codec.mysql.Packet
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.ProxyContext
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.constant.CapabilityFlag
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.constant.MySqlFieldType
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.readFixedLengthInteger
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.readLenEncInteger
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.readLenEncString
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.readRestOfPacketString
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler

// https://dev.mysql.com/doc/dev/mysql-server/latest/page_protocol_com_query.html
class QueryCommandHandler(
    private val proxyContext: ProxyContext,
) : SimpleChannelInboundHandler<Packet>() {
    override fun channelRead0(
        ctx: ChannelHandlerContext,
        msg: Packet,
    ) {
        val payload = msg.payload
        payload.markReaderIndex()

        // check if the first byte is 0x03 (COM_QUERY)
        if (payload.readByte() != 0x03.toByte()) {
            payload.resetReaderIndex()
            ctx.fireChannelRead(msg) // Not a COM_QUERY, pass it along
            return
        }
        logger.trace { "Received COM_QUERY" }

        if (proxyContext.capabilities().contains(CapabilityFlag.CLIENT_QUERY_ATTRIBUTES)) {
            val parameterCount = payload.readLenEncInteger().toInt()
            val parameterSetCount = payload.readLenEncInteger().toInt() // always 1 currently
            logger.trace { "Parameter count: $parameterCount, Parameter set count: $parameterSetCount" }
            if (parameterCount > 0) {
                val nullBitmap = payload.readString((parameterCount + 7) / 8, Charsets.UTF_8)
                val nextParamsBindFlag = payload.readFixedLengthInteger(1)
                if (nextParamsBindFlag != 1L) {
                    // malformed packet, unexpected nextParamsBindFlag
                    logger.warn { "Unexpected nextParamsBindFlag: $nextParamsBindFlag. Malformed packet" }
                }
                val parameters = mutableListOf<Triple<MySqlFieldType, String, Any?>>()
                for (i in 0 until parameterCount) {
                    val parameterTypeAndFlag = payload.readFixedLengthInteger(2)
                    val type = MySqlFieldType.of(parameterTypeAndFlag.toInt())
                    val parameterName = payload.readLenEncString()
                    parameters.add(Triple(type, parameterName.toString(Charsets.UTF_8), null))
                }
                logger.trace { "Parameters: $parameters" }
            }
        }

        val query = payload.readRestOfPacketString()
        logger.debug { "Query: ${query.toString(Charsets.UTF_8)}" }

        proxyContext.upstream().pipeline().addBefore(
            "relay-handler",
            "com-query-response-handler",
            QueryCommandResponseHandler(proxyContext),
        )
        ctx.pipeline().remove(this)

        payload.resetReaderIndex()
        proxyContext.upstream().writeAndFlush(msg)
    }

    override fun handlerAdded(ctx: ChannelHandlerContext) {
        logger.trace { this::class.simpleName + " added to pipeline" }
    }

    override fun handlerRemoved(ctx: ChannelHandlerContext) {
        logger.trace { this::class.simpleName + " removed from pipeline" }
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
