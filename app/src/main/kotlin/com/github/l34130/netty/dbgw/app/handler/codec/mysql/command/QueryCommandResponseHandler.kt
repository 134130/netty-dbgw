package com.github.l34130.netty.dbgw.app.handler.codec.mysql.command

import com.github.l34130.netty.dbgw.app.handler.codec.mysql.Packet
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.ProxyContext
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.constant.CapabilityFlag
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.constant.MySqlFieldType
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.readFixedLengthInteger
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.readFixedLengthString
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.readLenEncInteger
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.readLenEncString
import com.github.l34130.netty.dbgw.utils.netty.peek
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.buffer.ByteBuf
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

        if (handleTerminator(ctx, msg)) {
            return
        }

        // Process the Text Resultset
        if (columnCount == null) {
            if (msg.isOkPacket()) {
                // Handled query without result set
                val okPacket = Packet.Ok.readFrom(payload, proxyContext.capabilities())
                logger.trace { "Received COM_QUERY_RESPONSE: $okPacket" }
                msg.payload.resetReaderIndex()
                ctx.pipeline().remove(this)
                proxyContext.downstream().pipeline().addBefore("relay-handler", "com-query-handler", QueryCommandHandler(proxyContext))
                proxyContext.downstream().writeAndFlush(msg)
                return
            }

            val metadataFollows =
                if (proxyContext.capabilities().contains(CapabilityFlag.CLIENT_OPTIONAL_RESULTSET_METADATA)) {
                    when (payload.readFixedLengthInteger(1).toInt()) {
                        0 -> false // 0x00 means no metadata follows
                        1 -> true // 0x01 means full metadata follows
                        else -> error("Unexpected metadata follows flag value")
                    }
                } else {
                    null
                }
            logger.trace { "Metadata follows: $metadataFollows" }

            columnCount = payload.readLenEncInteger()
            logger.trace { "Column count: $columnCount" }

            if (metadataFollows == true) {
                for (i in 0 until columnCount!!) {
                    payload.readColumnDefinition() // Read each column definition
                }
            }
        }

        if (proxyContext.capabilities().contains(CapabilityFlag.CLIENT_DEPRECATE_EOF)) {
            val eofPacket = Packet.Eof.readFrom(payload, proxyContext.capabilities())
            logger.trace { "EOF Packet: $eofPacket" }
        }

        if (payload.readableBytes() > 0) {
            val row = payload.readTextResultsetRow(columnCount!!)
            logger.trace { "Row: $row" }
        }

        payload.resetReaderIndex()
        proxyContext.downstream().writeAndFlush(msg)
    }

    private fun handleTerminator(
        ctx: ChannelHandlerContext,
        msg: Packet,
    ): Boolean {
        if (msg.payload.peek { it.readUnsignedByte().toInt() == 0xFB } == true) {
            // TODO: handle more data
            logger.warn { "Unhandled 0xFB byte in COM_QUERY_RESPONSE, this indicates a More Data packet." }
            return false // Not a terminator packet
        }

        val packet =
            if (msg.isErrorPacket()) {
                Packet.Error.readFrom(msg.payload, proxyContext.capabilities())
            } else if (proxyContext.capabilities().contains(CapabilityFlag.CLIENT_DEPRECATE_EOF) && msg.isOkPacket()) {
                Packet.Ok.readFrom(msg.payload, proxyContext.capabilities())
            } else if (msg.isEofPacket()) {
                Packet.Eof.readFrom(msg.payload, proxyContext.capabilities())
            } else {
                return false // Not a terminator packet
            }
        logger.debug { "COM_QUERY_RESPONSE terminated: $packet" }

        msg.payload.resetReaderIndex()
        ctx.pipeline().remove(this)
        proxyContext.downstream().pipeline().addBefore("relay-handler", "com-query-handler", QueryCommandHandler(proxyContext))
        proxyContext.downstream().writeAndFlush(msg)
        return true
    }

    // https://dev.mysql.com/doc/dev/mysql-server/latest/page_protocol_com_query_response_text_resultset_column_definition.html
    private fun ByteBuf.readColumnDefinition() {
        val catalog = this.readLenEncString() // always "def"
        val schema = this.readLenEncString()
        val table = this.readLenEncString() // virtual table name
        val orgTable = this.readLenEncString() // physical table name
        val name = this.readLenEncString() // virtual column name
        val orgName = this.readLenEncString() // physical column name
        val lengthOfFixedLengthFields = this.readLenEncInteger() // [0x0C]
        val characterSet = this.readFixedLengthInteger(2).toInt()
        val columnLength = this.readFixedLengthInteger(4).toInt() // maximum length of the field
        val type = MySqlFieldType.of(this.readFixedLengthInteger(1).toInt())
        val flags = this.readFixedLengthInteger(2).toInt()
        val decimals = this.readFixedLengthInteger(1).toInt() // max shown decimal digits
        val reserved = this.readFixedLengthString(2)
        logger.trace {
            buildString {
                append("Column Definition: ")
                append("catalog=$catalog, schema=$schema, table=$table, orgTable=$orgTable, ")
                append("name=$name, orgName=$orgName, characterSet=$characterSet, ")
                append("columnLength=$columnLength, type=$type, flags=$flags, ")
            }
        }
    }

    // https://dev.mysql.com/doc/dev/mysql-server/latest/page_protocol_com_query_response_text_resultset_row.html
    private fun ByteBuf.readTextResultsetRow(columnCount: Long): List<String?>? {
        val result = mutableListOf<String?>()

        for (i in 0 until columnCount) {
            if (this.readableBytes() == 0) {
                logger.warn { "No more data to read for column $i, expected $columnCount columns" }
                return result
            }

            if (this.peek { it.readUnsignedByte() }?.toInt() == 0xFB) {
                this.skipBytes(1) // 0xFB indicates the end of the row
                // NULL
                result.add(null)
            } else {
                val value = this.readLenEncString()
                result.add(value.toString(Charsets.UTF_8))
            }
        }

        return result
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
