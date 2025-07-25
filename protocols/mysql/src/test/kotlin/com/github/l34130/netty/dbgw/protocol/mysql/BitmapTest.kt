package com.github.l34130.netty.dbgw.protocol.mysql

import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import kotlin.test.assertTrue

class BitmapTest {
    @TestFactory
    fun `test get`() =
        listOf(
            dynamicTest("9th field is a marked") {
                val bitmap = Bitmap(byteArrayOf(0x00, 0x01))
                val result = bitmap.get(8)
                assertTrue(result, "Expected 9th field to be marked as true, but got false")
            },
            dynamicTest("9th field is a marked with offset 2") {
                val bitmap = Bitmap(byteArrayOf(0x00, 0x04))
                val result = bitmap.get(8 + 2)
                assertTrue(result, "Expected 9th field to be marked as true, but got false")
            },
        )
}
