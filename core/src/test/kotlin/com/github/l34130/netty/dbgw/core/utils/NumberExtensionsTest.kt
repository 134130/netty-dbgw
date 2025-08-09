package com.github.l34130.netty.dbgw.core.utils

import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import kotlin.test.assertEquals

class NumberExtensionsTest {
    @TestFactory
    fun `test toHexString`() =
        listOf(
            dynamicTest("zero converts to '0x00'") {
                assertEquals("0x00", 0.toHexString())
            },
            dynamicTest("positive integer converts to hex") {
                assertEquals("0xFF", 255.toHexString())
            },
            dynamicTest("negative integer converts to hex") {
                assertEquals("0x0100", 256.toHexString())
            },
        )
}
