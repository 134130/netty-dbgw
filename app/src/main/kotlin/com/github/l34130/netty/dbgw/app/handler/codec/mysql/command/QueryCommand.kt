package com.github.l34130.netty.dbgw.app.handler.codec.mysql.command

import com.github.l34130.netty.dbgw.app.handler.codec.mysql.Capabilities
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.MySqlFieldType
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.Packet
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.ProxyContext
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.peek
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.readFixedLengthInteger
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.readFixedLengthString
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.readLenEncInteger
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.readLenEncString
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.readRestOfPacketString
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.buffer.ByteBuf
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

        if (proxyContext.clientCapabilities.hasFlag(Capabilities.CLIENT_QUERY_ATTRIBUTES)) {
            val parameterCount = payload.readLenEncInteger().toInt()
            val parameterSetCount = payload.readLenEncInteger().toInt() // always 1 currently
            if (parameterCount > 0) {
                val nullBitmap = payload.readFixedLengthString((parameterCount + 7) / 8)
                val nextParamsBindFlag = payload.readFixedLengthInteger(1).value
                if (nextParamsBindFlag != 1L) {
                    // malformed packet, unexpected nextParamsBindFlag
                    throw IllegalStateException("Unexpected nextParamsBindFlag: $nextParamsBindFlag")
                }
                val parameters = mutableListOf<Triple<MySqlFieldType, String, Any?>>()
                for (i in 0 until parameterCount) {
                    val parameterTypeAndFlag = payload.readFixedLengthInteger(2)
                    val type = MySqlFieldType.of(parameterTypeAndFlag.value.toInt())
                    val parameterName = payload.readLenEncString()
                    parameters.add(Triple(type, parameterName.toString(Charsets.UTF_8), null))
                }
                logger.trace { "Parameters: $parameters" }
            }
        }

        val query = payload.readRestOfPacketString()
        logger.trace { "Query: ${query.toString(Charsets.UTF_8)}" }

        payload.resetReaderIndex()
        proxyContext.upstream().pipeline().addBefore(
            "relay-handler",
            "query-command-response-handler",
            QueryCommandResponseHandler(proxyContext),
        )
        proxyContext.upstream().writeAndFlush(msg)
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}

// https://dev.mysql.com/doc/dev/mysql-server/latest/page_protocol_com_query_response.html
class QueryCommandResponseHandler(
    private val proxyContext: ProxyContext,
) : SimpleChannelInboundHandler<Packet>() {
    private var isFirstPacket = true
    private var columnCount: Long? = null

    override fun channelRead0(
        ctx: ChannelHandlerContext,
        msg: Packet,
    ) {
        if (isFirstPacket) {
            logger.trace { "Received COM_QUERY_RESPONSE" }
            isFirstPacket = false
        }

        if (msg.payload.peek { it.readUnsignedByte().toInt() == 0xFB } == true) {
            // TODO: handle more data
            logger.warn { "Unhandled 0xFB byte in COM_QUERY_RESPONSE, this indicates a More Data packet." }
        }

        val payload = msg.payload
        payload.markReaderIndex()

        // Process the Text Resultset

        if (columnCount == null) {
            val metadataFollows =
                if (proxyContext.clientCapabilities.hasFlag(Capabilities.CLIENT_OPTIONAL_RESULTSET_METADATA)) {
                    when (payload.readFixedLengthInteger(1).value.toInt()) {
                        0 -> false // 0x00 means no metadata follows
                        1 -> true // 0x01 means full metadata follows
                        else -> error("Unexpected metadata follows flag value")
                    }
                } else {
                    false
                }
            logger.trace { "Metadata follows: $metadataFollows" }

            columnCount = payload.readLenEncInteger()
            logger.trace { "Column count: $columnCount" }

            if (metadataFollows) {
                for (i in 0 until columnCount!!) {
                    payload.readColumnDefinition() // Read each column definition
                }
            }
        }

        if (!proxyContext.clientCapabilities.hasFlag(Capabilities.CLIENT_DEPRECATE_EOF)) {
            val eofPacket = Packet.Eof.readFrom(payload, proxyContext.clientCapabilities)
            logger.trace { "EOF Packet: $eofPacket" }
        }

        if (msg.isErrorPacket()) {
            logger.trace {
                "COM_QUERY_RESPONSE terminated with ERR_Packet - ${Packet.Error.readFrom(
                    payload,
                    proxyContext.clientCapabilities,
                )}"
            }
            payload.resetReaderIndex()
            ctx.pipeline().remove(this)
            proxyContext.downstream().writeAndFlush(msg)
            return
        }

        if (msg.isOkPacket()) {
            if (proxyContext.clientCapabilities.hasFlag(Capabilities.CLIENT_DEPRECATE_EOF)) {
                logger.trace {
                    "COM_QUERY_RESPONSE terminated with OK_Packet - ${Packet.Ok.readFrom(
                        payload,
                        proxyContext.clientCapabilities,
                    )}"
                }
            } else {
                logger.trace {
                    "COM_QUERY_RESPONSE terminated with EOF_Packet - ${Packet.Eof.readFrom(
                        payload,
                        proxyContext.clientCapabilities,
                    )}"
                }
            }
            payload.resetReaderIndex()
            ctx.pipeline().remove(this)
            proxyContext.downstream().writeAndFlush(msg)
            return
        }

        if (payload.readableBytes() > 0) {
            val row = payload.readTextResultsetRow(columnCount!!)
            logger.trace { "Row: $row" }
        }

        payload.resetReaderIndex()
        proxyContext.downstream().writeAndFlush(msg)
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
        val characterSet = this.readFixedLengthInteger(2).value.toInt()
        val columnLength = this.readFixedLengthInteger(4).value.toInt() // maximum length of the field
        val type = MySqlFieldType.of(this.readFixedLengthInteger(1).value.toInt())
        val flags = this.readFixedLengthInteger(2).value.toInt()
        val decimals = this.readFixedLengthInteger(1).value.toInt() // max shown decimal digits
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
            if (this.readableBytes() <= 0) {
                logger.warn { "No more data to read for column $i, expected $columnCount columns." }
                return null // No more data to read
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

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
