package com.github.l34130.netty.dbgw.policy.builtin.database.query

import com.github.l34130.netty.dbgw.policy.api.Policy
import com.github.l34130.netty.dbgw.policy.api.query.AbstractDatabaseQueryPolicy
import com.github.l34130.netty.dbgw.policy.api.query.DatabaseQueryContext
import com.github.l34130.netty.dbgw.policy.api.query.DatabaseQueryPolicyResult
import java.time.Clock
import java.time.LocalTime
import java.util.regex.Pattern

// TODO: Make this policy as not query policy, but as a connection policy
@Policy(
    group = "builtin",
    version = "v1",
    kind = "DatabaseTimeRangeAccessQueryPolicy",
    plural = "databasetimerangeaccessquerypolicies",
    singular = "databasetimerangeaccessquerypolicy",
)
data class DatabaseTimeRangeAccessQueryPolicy(
    private val startTime: LocalTime,
    private val endTime: LocalTime,
    private val startInclusive: Boolean,
    private val endInclusive: Boolean,
    private val allowInRange: Boolean = true,
    private val clock: Clock,
) : AbstractDatabaseQueryPolicy() {
    private val rangeNotation: String =
        "${if (startInclusive) '[' else '('}$startTime, $endTime${if (endInclusive) ']' else ')'}"

    init {
        require(startTime != endTime) {
            "Start and end time must not be the same"
        }
    }

    override fun evaluate(ctx: DatabaseQueryContext): DatabaseQueryPolicyResult {
        val currentTime = LocalTime.now(clock)

        val afterStart = if (startInclusive) !currentTime.isBefore(startTime) else currentTime.isAfter(startTime)
        val beforeEnd = if (endInclusive) !currentTime.isAfter(endTime) else currentTime.isBefore(endTime)

        val isWithinRange =
            if (startTime.isBefore(endTime)) {
                // 09:00 ~ 17:00
                afterStart && beforeEnd
            } else {
                // 22:00 ~ 02:00 (crossing midnight)
                afterStart || beforeEnd
            }

        val shouldAllow = if (allowInRange) isWithinRange else !isWithinRange
        return if (shouldAllow) {
            DatabaseQueryPolicyResult.Allowed()
        } else {
            DatabaseQueryPolicyResult.Denied(
                reason =
                    if (allowInRange) {
                        "current time is outside the allowed range $rangeNotation"
                    } else {
                        "current time is within the blocked range $rangeNotation"
                    },
            )
        }
    }

    companion object {
        private val RANGE_PATTERN: Pattern =
            Pattern.compile("""^([(\[])\s*(\d{2}:\d{2})\s*,\s*(\d{2}:\d{2})\s*([)\]])$""")

        /**
         * Creates a [DatabaseTimeRangeAccessQueryPolicy] from a string representation of a time range
         * @param range the time range string in the format `[HH:mm, HH:mm)`, `(HH:mm, HH:mm)`, `[HH:mm, HH:mm]`, or `(HH:mm, HH:mm]`
         * @param allowInRange if true, the policy allows access within the specified range; if false, it denies access within the specified range
         */
        fun from(
            range: String,
            allowInRange: Boolean = true,
            clock: Clock = Clock.systemDefaultZone(),
        ): DatabaseTimeRangeAccessQueryPolicy {
            val matcher = RANGE_PATTERN.matcher(range)
            require(matcher.matches()) { "Range must be in format '[HH:mm, HH:mm)' or similar" }

            val startBracket = matcher.group(1)
            val startTimeStr = matcher.group(2)
            val endTimeStr = matcher.group(3)
            val endBracket = matcher.group(4)

            return DatabaseTimeRangeAccessQueryPolicy(
                startTime = LocalTime.parse(startTimeStr),
                endTime = LocalTime.parse(endTimeStr),
                startInclusive = startBracket == "[",
                endInclusive = endBracket == "]",
                allowInRange = allowInRange,
                clock = clock,
            )
        }
    }
}
