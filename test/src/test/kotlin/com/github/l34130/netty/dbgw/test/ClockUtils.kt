package com.github.l34130.netty.dbgw.test

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

object ClockUtils {
    /**
     * Gets an instance of Instant from a text string such as 2007-12-03T10:15:30.00Z.
     * The string must represent a valid instant in UTC and is parsed using DateTimeFormatter.ISO_INSTANT.
     */
    fun fixed(text: String): Clock = Clock.fixed(Instant.parse(text), ZoneId.of("UTC"))
}
