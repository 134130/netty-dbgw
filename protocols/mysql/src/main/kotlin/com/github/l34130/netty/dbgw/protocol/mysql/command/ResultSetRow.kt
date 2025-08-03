package com.github.l34130.netty.dbgw.protocol.mysql.command

import com.github.l34130.netty.dbgw.core.utils.netty.peek
import com.github.l34130.netty.dbgw.protocol.mysql.Packet
import com.github.l34130.netty.dbgw.protocol.mysql.readLenEncString
import com.github.l34130.netty.dbgw.protocol.mysql.writeLenEncString
import io.netty.buffer.Unpooled

internal data class ResultSetRow(
    val columns: List<String?> = emptyList(),
) {
    fun asPacket(sequenceId: Int): Packet =
        Packet(
            sequenceId = sequenceId,
            payload =
                Unpooled.buffer().apply {
                    columns.forEach { column ->
                        if (column == null) {
                            this.writeByte(0xFB) // 0xFB indicates NULL value
                        } else {
                            this.writeLenEncString(column)
                        }
                    }
                },
        )

    companion object {
        fun readFrom(
            packet: Packet,
            columnCount: ULong,
        ): ResultSetRow {
            val result = mutableListOf<String?>()

            for (i in 0UL until columnCount) {
                if (packet.payload.readableBytes() == 0) {
                    break
                }

                if (packet.payload.peek { it.readUnsignedByte() }?.toInt() == 0xFB) {
                    packet.payload.skipBytes(1) // 0xFB indicates NULL value
                    // NULL
                    result.add(null)
                } else {
                    val value = packet.payload.readLenEncString()
                    result.add(value.toString(Charsets.UTF_8))
                }
            }

            return ResultSetRow(
                columns = result,
            )
        }
    }
}
