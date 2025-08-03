package com.github.l34130.netty.dbgw.policy.builtin.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IntRangeSetTest {
    @Test
    fun `add merges overlapping ranges`() {
        val rangeSet = IntRangeSet()
        rangeSet.add(1..5)
        rangeSet.add(4..10)

        assertTrue(rangeSet.contains(3))
        assertTrue(rangeSet.contains(6))
        assertTrue(rangeSet.contains(10))
        assertFalse(rangeSet.contains(11))
        assertEquals(1, rangeSet.ranges().size)
    }

    @Test
    fun `add keeps non-overlapping ranges separate`() {
        val rangeSet = IntRangeSet()
        rangeSet.add(1..5)
        rangeSet.add(7..10)

        assertTrue(rangeSet.contains(1))
        assertTrue(rangeSet.contains(8))
        assertFalse(rangeSet.contains(6))
        assertEquals(2, rangeSet.ranges().size)
    }
}
