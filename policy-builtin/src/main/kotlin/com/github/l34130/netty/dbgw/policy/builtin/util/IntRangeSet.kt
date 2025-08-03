package com.github.l34130.netty.dbgw.policy.builtin.util

import org.jetbrains.annotations.VisibleForTesting

class IntRangeSet : Iterable<Int> {
    private val ranges = mutableListOf<IntRange>()

    fun add(range: IntRange) {
        ranges.add(range)
        ranges.sortBy { it.first }
        mergeOverlappingRanges()
    }

    fun contains(value: Int): Boolean = ranges.any { value in it }

    @VisibleForTesting
    fun ranges(): List<IntRange> = ranges.toList()

    private fun mergeOverlappingRanges() {
        val merged = mutableListOf<IntRange>()
        var currentRange: IntRange? = null

        for (range in ranges) {
            if (currentRange == null) {
                currentRange = range
            } else if (currentRange.last >= range.first - 1) {
                currentRange = IntRange(currentRange.first, maxOf(currentRange.last, range.last))
            } else {
                merged.add(currentRange)
                currentRange = range
            }
        }

        currentRange?.let { merged.add(it) }
        ranges.clear()
        ranges.addAll(merged)
    }

    override fun iterator(): Iterator<Int> = ranges.flatMap { it.asIterable() }.iterator()

    override fun toString(): String = ranges.joinToString(", ") { "[${it.first}, ${it.last}]" }
}
