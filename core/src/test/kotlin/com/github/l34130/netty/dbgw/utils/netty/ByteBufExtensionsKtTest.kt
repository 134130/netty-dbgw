package com.github.l34130.netty.dbgw.utils.netty

import com.github.l34130.netty.dbgw.core.util.netty.peek
import com.github.l34130.netty.dbgw.core.util.netty.readAndReplace
import com.github.l34130.netty.dbgw.core.util.netty.replace
import io.netty.buffer.Unpooled
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ByteBufExtensionsKtTest {
    @TestFactory
    fun peekTest(): List<DynamicNode> =
        listOf(
            dynamicTest("peek returns null when ByteBuf is empty") {
                val emptyBuf = Unpooled.buffer(0)
                val result = emptyBuf.peek { it.readByte() }
                assertNull(result, "Expected null when ByteBuf is empty")
            },
            dynamicTest("peek returns result of action without modifying reader index") {
                val buf = Unpooled.buffer().writeBytes(byteArrayOf(1, 2, 3))
                val initialReaderIndex = buf.readerIndex()
                val result = buf.peek { it.readByte() }
                assertEquals(1.toByte(), result, "Expected action to return the first byte")
                assertEquals(initialReaderIndex, buf.readerIndex(), "Reader index should not be modified")
            },
            dynamicTest("peek handles IndexOutOfBoundsException gracefully and returns null") {
                val buf = Unpooled.buffer().writeBytes(byteArrayOf(1))
                val result = buf.peek { it.readBytes(10) }
                assertNull(result, "Expected null when IndexOutOfBoundsException occurs")
            },
            dynamicTest("peek allows multiple calls without consuming data") {
                val buf = Unpooled.buffer().writeBytes(byteArrayOf(1, 2, 3))
                val firstPeek = buf.peek { it.readByte() }
                val secondPeek = buf.peek { it.readByte() }
                assertEquals(1.toByte(), firstPeek, "First peek should return the first byte")
                assertEquals(1.toByte(), secondPeek, "Second peek should also return the first byte")
            },
        )

    @TestFactory
    fun replaceTest(): List<DynamicNode> =
        listOf(
            dynamicTest("replace returns original ByteBuf when block returns null") {
                val buf = Unpooled.buffer().writeBytes(byteArrayOf(1, 2, 3))
                val result = buf.readAndReplace { null }
                assertEquals(buf, result, "Expected original ByteBuf when block returns null")
            },
            dynamicTest("replace returns original ByteBuf when block reads no bytes") {
                val buf = Unpooled.buffer().writeBytes(byteArrayOf(1, 2, 3))
                val result = buf.readAndReplace { Unpooled.buffer(0) }
                assertEquals(buf, result, "Expected original ByteBuf when block reads no bytes")
            },
            dynamicTest("replace replaces specified portion with new data") {
                val buf = Unpooled.buffer().writeBytes(byteArrayOf(1, 2, 3, 4, 5))
                val result =
                    buf.readAndReplace {
                        it.readBytes(2)
                        Unpooled.wrappedBuffer(byteArrayOf(9, 9))
                    }
                val expected = Unpooled.wrappedBuffer(byteArrayOf(9, 9, 3, 4, 5))
                assertEquals(expected, result, "Expected ByteBuf with replaced portion")
            },
            dynamicTest("replace throws IndexOutOfBoundsException for invalid index or length") {
                val buf = Unpooled.buffer().writeBytes(byteArrayOf(1, 2, 3))
                assertFailsWith<IndexOutOfBoundsException> {
                    buf.replace(-1, 2, Unpooled.wrappedBuffer(byteArrayOf(9)))
                }
                assertFailsWith<IndexOutOfBoundsException> {
                    buf.replace(1, 5, Unpooled.wrappedBuffer(byteArrayOf(9)))
                }
            },
            dynamicTest("replace handles empty ByteBuf gracefully") {
                val buf = Unpooled.buffer(0)
                val result = buf.readAndReplace { Unpooled.wrappedBuffer(byteArrayOf(9)) }
                assertEquals(buf, result, "Expected original ByteBuf when ByteBuf is empty")
            },
            dynamicTest("replace throws IndexOutOfBoundsException when trying to read more bytes than available") {
                val buf = Unpooled.buffer().writeBytes(byteArrayOf(1, 2, 3))
                assertFailsWith<IndexOutOfBoundsException> {
                    buf.readAndReplace {
                        it.readBytes(5) // Attempt to read more bytes than available
                    }
                }
            },
            dynamicTest("replace allows writing more bytes than read") {
                val buf = Unpooled.buffer().writeBytes(byteArrayOf(1, 2, 3))
                val result =
                    buf.readAndReplace {
                        it.skipBytes(3) // Read all bytes
                        Unpooled.buffer().writeBytes(byteArrayOf(1, 2, 3, 4, 5)) // Attempt to write more bytes than available
                    }
                assertEquals(
                    Unpooled.buffer().writeBytes(byteArrayOf(1, 2, 3, 4, 5)),
                    result,
                    "Expected ByteBuf with replaced portion",
                )
            },
        )
}
