package com.github.l34130.netty.dbgw.app.handler.codec.mysql.command

import com.github.l34130.netty.dbgw.app.handler.codec.mysql.Packet
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.ProxyContext
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.constant.CapabilityFlag
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.constant.MySqlFieldType
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.constant.ServerStatusFlag
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.readFixedLengthInteger
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.readFixedLengthString
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.readLenEncInteger
import com.github.l34130.netty.dbgw.app.handler.codec.mysql.readLenEncString
import com.github.l34130.netty.dbgw.utils.netty.peek
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler

class QueryCommandResponseTextResultsetHandler(
    private val proxyContext: ProxyContext,
) : SimpleChannelInboundHandler<Packet>() {
    private var state: State = State.COLUMN_COUNT
    private var columnCount: ULong = 0UL
    private var metadataFollows: Boolean = false
    private var columnDefinitionCount: ULong = 0UL

    override fun channelRead0(
        ctx: ChannelHandlerContext,
        msg: Packet,
    ) {
        var handled = false
        while (state != State.TERMINATED && !handled) {
            handled =
                when (state) {
                    State.COLUMN_COUNT -> handleColumnCountState(ctx, msg)
                    State.COLUMN_DEFINITION -> handleColumnDefinitionState(ctx, msg)
                    State.EOF -> handleEofState(ctx, msg)
                    State.ROW_DATA -> handleRowDataState(ctx, msg)
                    State.TERMINATED -> true
                }
        }
    }

    private fun handleColumnCountState(
        ctx: ChannelHandlerContext,
        msg: Packet,
    ): Boolean {
        val payload = msg.payload
        payload.markReaderIndex()

        if (proxyContext.capabilities().contains(CapabilityFlag.CLIENT_OPTIONAL_RESULTSET_METADATA)) {
            metadataFollows = payload.readFixedLengthInteger(1) == 1UL
        }
        this.columnCount = payload.readLenEncInteger()

        this.state = State.COLUMN_DEFINITION

        payload.resetReaderIndex()
        proxyContext.downstream().writeAndFlush(msg)
        return true
    }

    // https://dev.mysql.com/doc/dev/mysql-server/latest/page_protocol_com_query_response_text_resultset_column_definition.html
    private fun handleColumnDefinitionState(
        ctx: ChannelHandlerContext,
        msg: Packet,
    ): Boolean {
        val payload = msg.payload
        payload.markReaderIndex()

        logger.trace {
            val catalog = payload.readLenEncString().toString(Charsets.UTF_8) // always "def"
            val schema = payload.readLenEncString().toString(Charsets.UTF_8)
            val table = payload.readLenEncString().toString(Charsets.UTF_8) // virtual table name
            val orgTable = payload.readLenEncString().toString(Charsets.UTF_8) // physical table name
            val name = payload.readLenEncString().toString(Charsets.UTF_8) // virtual column name
            val orgName = payload.readLenEncString().toString(Charsets.UTF_8) // physical column name
            val lengthOfFixedLengthFields = payload.readLenEncInteger() // [0x0C]
            val characterSet = payload.readFixedLengthInteger(2).toInt()
            val columnLength = payload.readFixedLengthInteger(4).toInt() // maximum length of the field
            val type = MySqlFieldType.of(payload.readFixedLengthInteger(1).toInt())
            val flags = payload.readFixedLengthInteger(2).toInt()
            val decimals = payload.readFixedLengthInteger(1).toInt() // max shown decimal digits
            val reserved = payload.readFixedLengthString(2)

            buildString {
                append("Column Definition(")
                append("catalog=$catalog, schema=$schema, table=$table, orgTable=$orgTable, ")
                append("name=$name, orgName=$orgName, characterSet=$characterSet, ")
                append("columnLength=$columnLength, type=$type, flags=$flags)")
            }
        }

        if (++columnDefinitionCount < columnCount) {
        } else {
            state = State.EOF
        }

        payload.resetReaderIndex()
        proxyContext.downstream().writeAndFlush(msg)
        return true
    }

    private fun handleEofState(
        ctx: ChannelHandlerContext,
        msg: Packet,
    ): Boolean {
        if (!proxyContext.capabilities().contains(CapabilityFlag.CLIENT_OPTIONAL_RESULTSET_METADATA)) {
            this.state = State.ROW_DATA
            return false
        }

        logger.trace {
            val eofPacket =
                msg.payload.peek {
                    Packet.Eof.readFrom(it, proxyContext.capabilities())
                }
            "End of metadata reached: $eofPacket"
        }
        this.state = State.ROW_DATA

        proxyContext.downstream().writeAndFlush(msg)
        return true
    }

    private fun handleRowDataState(
        ctx: ChannelHandlerContext,
        msg: Packet,
    ): Boolean {
        val payload = msg.payload
        payload.markReaderIndex()

        if (handleTerminator(ctx, msg)) {
            this.state = State.TERMINATED
            return true
        }

        val rowData = payload.readTextResultsetRow(columnCount)
//        logger.trace { "Row Data: $rowData" }

        payload.resetReaderIndex()
        proxyContext.downstream().writeAndFlush(msg)
        return true
    }

    private fun handleTerminator(
        ctx: ChannelHandlerContext,
        msg: Packet,
    ): Boolean {
        if (msg.isErrorPacket()) {
            val packet = Packet.Error.readFrom(msg.payload, proxyContext.capabilities())
            logger.debug { "Text Resultset terminated: $packet" }

            msg.payload.resetReaderIndex()
            ctx.pipeline().remove(this)
            proxyContext.downstream().pipeline().addBefore("relay-handler", "com-query-handler", QueryCommandHandler(proxyContext))
            proxyContext.downstream().writeAndFlush(msg)
            return true
        }

        val moreResultsExists =
            if (proxyContext.capabilities().contains(CapabilityFlag.CLIENT_DEPRECATE_EOF) && msg.isOkPacket()) {
                val packet = Packet.Ok.readFrom(msg.payload, proxyContext.capabilities())
                logger.debug { "Text Resultset terminated: $packet" }
                packet.statusFlags?.contains(ServerStatusFlag.SERVER_MORE_RESULTS_EXISTS) == true
            } else if (msg.isEofPacket()) {
                val packet = Packet.Eof.readFrom(msg.payload, proxyContext.capabilities())
                logger.debug { "Text Resultset terminated: $packet" }
                packet.statusFlags?.contains(ServerStatusFlag.SERVER_MORE_RESULTS_EXISTS) == true
            } else {
                return false // Not a terminator packet
            }

        if (moreResultsExists) {
            logger.debug { "More results exist, continuing to new Text Resultset" }
            ctx.pipeline().replace(
                this,
                "com-query-response-text-resultset-handler",
                QueryCommandResponseTextResultsetHandler(proxyContext),
            )
        } else {
            ctx.pipeline().remove(this)
            proxyContext.downstream().pipeline().addBefore("relay-handler", "com-query-handler", QueryCommandHandler(proxyContext))
        }

        msg.payload.resetReaderIndex()
        proxyContext.downstream().writeAndFlush(msg)
        return true
    }

    // https://dev.mysql.com/doc/dev/mysql-server/latest/page_protocol_com_query_response_text_resultset_row.html
    private fun ByteBuf.readTextResultsetRow(columnCount: ULong): List<String?> {
        val result = mutableListOf<String?>()

        for (i in 0UL until columnCount) {
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
        private val logger = KotlinLogging.logger { }
    }

    private enum class State {
        COLUMN_COUNT,
        COLUMN_DEFINITION,
        EOF,
        ROW_DATA,
        TERMINATED,
    }
}
