package com.github.l34130.netty.dbgw.app.handler.codec.mysql.command

import com.github.l34130.netty.dbgw.app.handler.codec.mysql.Packet
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.ProxyContext
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler

// https://dev.mysql.com/doc/dev/mysql-server/latest/page_protocol_com_query_response.html
class QueryCommandResponseHandler(
    private val proxyContext: ProxyContext,
) : SimpleChannelInboundHandler<Packet>() {
    private var columnCount: Long? = null

    override fun channelRead0(
        ctx: ChannelHandlerContext,
        msg: Packet,
    ) {
        val payload = msg.payload
        payload.markReaderIndex()

        if (msg.isOkPacket()) {
            logger.trace {
                val okPacket = Packet.Ok.readFrom(payload, proxyContext.capabilities())
                "Received COM_QUERY_RESPONSE: $okPacket"
            }
            ctx.pipeline().remove(this)
            proxyContext.downstream().apply {
                pipeline().addBefore("relay-handler", "com-query-handler", QueryCommandHandler(proxyContext))
                msg.payload.resetReaderIndex()
                writeAndFlush(msg)
            }
            return
        }

        if (msg.isErrorPacket()) {
            logger.trace {
                val errorPacket = Packet.Error.readFrom(payload, proxyContext.capabilities())
                "Received COM_QUERY_RESPONSE: $errorPacket"
            }
            ctx.pipeline().remove(this)
            proxyContext.downstream().apply {
                pipeline().addBefore("relay-handler", "com-query-handler", QueryCommandHandler(proxyContext))
                msg.payload.resetReaderIndex()
                writeAndFlush(msg)
            }
            return
        }

        // TODO: 0xFB indicates a More Data packet, which is not handled here
//        logger.warn {
//            "Unhandled 0xFB byte in COM_QUERY_RESPONSE, this indicates a More Data packet."
//        }

        ctx.pipeline().addAfter(
            "com-query-response-handler",
            "com-query-response-text-resultset-handler",
            QueryCommandResponseTextResultsetHandler(proxyContext),
        )
        ctx.pipeline().remove(this)
        msg.payload.resetReaderIndex()
        ctx.fireChannelRead(msg)
        return
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
