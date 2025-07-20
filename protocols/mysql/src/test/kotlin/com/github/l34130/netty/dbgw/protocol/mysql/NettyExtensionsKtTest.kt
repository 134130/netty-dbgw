package com.github.l34130.netty.dbgw.protocol.mysql

import io.netty.buffer.Unpooled
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NettyExtensionsKtTest {
    @TestFactory
    fun `test ByteBuf readFixedLengthInteger`(): List<DynamicNode> =
        listOf(
            dynamicTest("negative length throws") {
                val buf = Unpooled.buffer()
                assertFailsWith<IllegalArgumentException> {
                    buf.readFixedLengthInteger(-1)
                }
            },
            dynamicTest("0 length throws") {
                val buf = Unpooled.buffer()
                assertFailsWith<IllegalArgumentException> {
                    buf.readFixedLengthInteger(0)
                }
            },
            dynamicTest("1 byte length reads correct value") {
                val buf = Unpooled.copiedBuffer(byteArrayOf(0x01))
                val result = buf.readFixedLengthInteger(1)
                assertEquals(0x01UL, result)
                assertEquals(1UL, result)
            },
            dynamicTest("2 byte length reads correct value") {
                val buf = Unpooled.copiedBuffer(byteArrayOf(0x01, 0x02))
                val result = buf.readFixedLengthInteger(2)
                assertEquals(0x0201UL, result)
                assertEquals(513UL, result)
            },
            dynamicTest("3 byte length reads correct value") {
                val buf = Unpooled.copiedBuffer(byteArrayOf(0x01, 0x02, 0x03))
                val result = buf.readFixedLengthInteger(3)
                assertEquals(0x030201UL, result)
                assertEquals(197121UL, result)
            },
            dynamicTest("4 byte length reads correct value") {
                val buf = Unpooled.copiedBuffer(byteArrayOf(0x01, 0x02, 0x03, 0x04))
                val result = buf.readFixedLengthInteger(4)
                assertEquals(0x04030201UL, result)
                assertEquals(67305985UL, result)
            },
            dynamicTest("5 length throws") {
                val buf = Unpooled.buffer()
                assertFailsWith<IllegalArgumentException> {
                    buf.readFixedLengthInteger(5)
                }
            },
            dynamicTest("6 byte length reads correct value") {
                val buf = Unpooled.copiedBuffer(byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06))
                val result = buf.readFixedLengthInteger(6)
                assertEquals(0x060504030201UL, result)
                assertEquals(6618611909121UL, result)
            },
            dynamicTest("7 length throws") {
                val buf = Unpooled.buffer()
                assertFailsWith<IllegalArgumentException> {
                    buf.readFixedLengthInteger(7)
                }
            },
            dynamicTest("8 byte length reads correct value") {
                val buf = Unpooled.copiedBuffer(byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08))
                val result = buf.readFixedLengthInteger(8)
                assertEquals(0x0807060504030201UL, result)
                assertEquals(578437695752307201UL, result)
            },
            dynamicTest("length greater than 8 throws") {
                val buf = Unpooled.buffer()
                assertFailsWith<IllegalArgumentException> {
                    buf.readFixedLengthInteger(9)
                }
            },
        )

    @TestFactory
    fun `test ByteBuf readLenEncInteger`(): List<DynamicNode> =
        listOf(
            dynamicTest("0x00 reads 0") {
                val buf = Unpooled.copiedBuffer(byteArrayOf(0x00))
                val result = buf.readLenEncInteger()
                assertEquals(0UL, result)
            },
            dynamicTest("0x01 reads 1") {
                val buf = Unpooled.copiedBuffer(byteArrayOf(0x01))
                val result = buf.readLenEncInteger()
                assertEquals(1UL, result)
            },
            dynamicTest("0xFB reads 251") {
                val buf = Unpooled.copiedBuffer(byteArrayOf(0xFB.toByte()))
                val result = buf.readLenEncInteger()
                assertEquals(251UL, result)
            },
            dynamicTest("0xFC followed by 2 bytes reads 252") {
                val buf = Unpooled.copiedBuffer(byteArrayOf(0xFC.toByte(), 0xFF.toByte(), 0x00))
                val result = buf.readLenEncInteger()
                assertEquals(0xFFUL, result)
                assertEquals(255UL, result)
            },
            dynamicTest("0xFD followed by 3 bytes reads 65535") {
                val buf = Unpooled.copiedBuffer(byteArrayOf(0xFD.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0x00))
                val result = buf.readLenEncInteger()
                assertEquals(0xFFFFUL, result)
                assertEquals(65535UL, result)
            },
            dynamicTest("0xFE followed by 8 bytes reads 4294967295") {
                val buf =
                    Unpooled.copiedBuffer(
                        byteArrayOf(
                            0xFE.toByte(),
                            0xFF.toByte(),
                            0xFF.toByte(),
                            0xFF.toByte(),
                            0xFF.toByte(),
                            0xFF.toByte(),
                            0xFF.toByte(),
                            0xFF.toByte(),
                            0xFF.toByte(),
                        ),
                    )
                val result = buf.readLenEncInteger()
                assertEquals(0xFFFFFFFFFFFFFFFFUL, result)
                assertEquals(18446744073709551615UL, result)
            },
            dynamicTest("invalid prefix throws") {
                val buf = Unpooled.copiedBuffer(byteArrayOf(0xFF.toByte()))
                assertFailsWith<IllegalArgumentException> {
                    buf.readLenEncInteger()
                }
            },
        )

    @TestFactory
    fun `test ByteBuf readFixedLengthString`(): List<DynamicNode> =
        listOf(
            dynamicTest("read string") {
                val buf = Unpooled.copiedBuffer("test", Charsets.US_ASCII)
                val result = buf.readFixedLengthString(4)
                assertEquals("test", result)
            },
            dynamicTest("read string with length less than available") {
                val buf = Unpooled.copiedBuffer("test", Charsets.US_ASCII)
                val result = buf.readFixedLengthString(3)
                assertEquals("tes", result)
                assertEquals(1, buf.readableBytes()) // Ensure only 1 byte remains
                val remainingStr = buf.readFixedLengthString(1)
                assertEquals("t", remainingStr)
                assertEquals(0, buf.readableBytes()) // Ensure no bytes remain
            },
            dynamicTest("read string with length more than available") {
                val buf = Unpooled.copiedBuffer("test", Charsets.US_ASCII)
                assertFailsWith<IndexOutOfBoundsException> {
                    buf.readFixedLengthString(5)
                }
            },
            dynamicTest("read negative length throws") {
                val buf = Unpooled.buffer()
                assertFailsWith<IndexOutOfBoundsException> {
                    buf.readFixedLengthString(-1)
                }
            },
        )

    @TestFactory
    fun `test ByteBuf readNullTerminatedString`(): List<DynamicNode> =
        listOf(
            dynamicTest("read null-terminated string") {
                val buf = Unpooled.copiedBuffer("test$NULL_CHAR", Charsets.US_ASCII)
                val result = buf.readNullTerminatedString()
                assertEquals("test", result.toString(Charsets.US_ASCII))
            },
            dynamicTest("read empty null-terminated string") {
                val buf = Unpooled.copiedBuffer("$NULL_CHAR", Charsets.US_ASCII)
                val result = buf.readNullTerminatedString()
                assertEquals("", result.toString(Charsets.US_ASCII))
            },
            dynamicTest("no null terminator throws") {
                val buf = Unpooled.copiedBuffer("test", Charsets.US_ASCII)
                val error =
                    assertFailsWith<IndexOutOfBoundsException> {
                        buf.readNullTerminatedString()
                    }
                assertEquals("No null terminator found in ByteBuf", error.message)
            },
        )

    @TestFactory
    fun `test ByteBuf readRestOfPacketString`(): List<DynamicNode> =
        listOf(
            dynamicTest("read empty string") {
                val buf = Unpooled.buffer()
                val result = buf.readRestOfPacketString()
                assertEquals("", result.toString(Charsets.UTF_8))
            },
            dynamicTest("read non-empty string") {
                val buf = Unpooled.copiedBuffer("test", Charsets.UTF_8)
                val result = buf.readRestOfPacketString()
                assertEquals("test", result.toString(Charsets.UTF_8))
            },
            dynamicTest("read string with null terminator") {
                val buf = Unpooled.copiedBuffer("test$NULL_CHAR", Charsets.UTF_8)
                val result = buf.readRestOfPacketString()
                assertEquals("test$NULL_CHAR", result.toString(Charsets.UTF_8))
            },
        )

    @TestFactory
    fun `test ByteBuf readLenEncString`(): List<DynamicNode> =
        listOf(
            dynamicTest("read empty string") {
                val buf = Unpooled.copiedBuffer(byteArrayOf(0x00))
                val result = buf.readLenEncString()
                assertEquals("", result.toString(Charsets.UTF_8))
            },
            dynamicTest("read short string") {
                val buf = Unpooled.copiedBuffer(byteArrayOf(0x05) + "hello".toByteArray(Charsets.UTF_8))
                val result = buf.readLenEncString()
                assertEquals("hello", result.toString(Charsets.UTF_8))
            },
            dynamicTest("read long string") {
                val longString = "a".repeat(1000)
                val length = longString.length.toULong()
                val buf =
                    Unpooled.buffer().apply {
                        writeByte(0xFC) // Use 0xFC for length encoding
                        writeShortLE(length.toInt()) // Write length as 2 bytes
                        writeBytes(longString.toByteArray(Charsets.UTF_8)) // Write the string
                    }
                val result = buf.readLenEncString()
                assertEquals(longString, result.toString(Charsets.UTF_8))
            },
        )

    companion object {
        private const val NULL_CHAR = '\u0000'
    }
}
