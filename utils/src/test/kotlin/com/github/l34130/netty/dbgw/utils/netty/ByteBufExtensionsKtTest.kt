import com.github.l34130.netty.dbgw.utils.netty.peek
import io.netty.buffer.Unpooled
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import kotlin.test.assertEquals
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
}
