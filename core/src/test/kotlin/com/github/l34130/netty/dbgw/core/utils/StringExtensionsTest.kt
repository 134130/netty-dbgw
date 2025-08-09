package com.github.l34130.netty.dbgw.core.utils

import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import kotlin.test.assertEquals

class StringExtensionsTest {
    @TestFactory
    fun `test ellipsize`(): List<DynamicTest?> =
        listOf(
            dynamicTest("short string does not change") {
                val shortString = "Short"
                assertEquals("Short", shortString.ellipsize(20))
            },
            dynamicTest("long string is truncated with ellipsis") {
                val longString = "This is a very long string that should be truncated."
                assertEquals("This is a very long…", longString.ellipsize(20))
            },
        )
}
