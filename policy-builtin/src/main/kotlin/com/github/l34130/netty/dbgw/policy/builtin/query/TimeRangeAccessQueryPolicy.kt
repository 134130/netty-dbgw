package com.github.l34130.netty.dbgw.policy.builtin.query

import com.github.l34130.netty.dbgw.policy.api.PolicyMetadata
import com.github.l34130.netty.dbgw.policy.api.query.QueryPolicy
import com.github.l34130.netty.dbgw.policy.api.query.QueryPolicyContext
import com.github.l34130.netty.dbgw.policy.api.query.QueryPolicyResult
import java.time.Clock
import java.time.LocalTime
import java.util.regex.Pattern

class TimeRangeAccessQueryPolicy(
    private val startTime: LocalTime,
    private val endTime: LocalTime,
    private val startInclusive: Boolean,
    private val endInclusive: Boolean,
    private val allowInRange: Boolean = true,
    private val clock: Clock,
) : QueryPolicy {
    private val rangeNotation: String =
        "${if (startInclusive) '[' else '('}$startTime, $endTime${if (endInclusive) ']' else ')'}"

    init {
        require(startTime != endTime) {
            "Start and end time must not be the same"
        }
    }

    override fun evaluate(
        ctx: QueryPolicyContext,
        query: String,
    ): QueryPolicyResult {
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
            QueryPolicyResult.Allowed()
        } else {
            QueryPolicyResult.Denied(
                reason =
                    if (allowInRange) {
                        "current time is outside the allowed range $rangeNotation"
                    } else {
                        "current time is within the blocked range $rangeNotation"
                    },
            )
        }
    }

    override fun getMetadata(): PolicyMetadata =
        PolicyMetadata(
            group = "builtin",
            version = "v1",
            kind = "TimeRangeAccessQueryPolicy",
        )

    companion object {
        private val RANGE_PATTERN: Pattern =
            Pattern.compile("""^([(\[])\s*(\d{2}:\d{2})\s*,\s*(\d{2}:\d{2})\s*([)\]])$""")

        /**
         * Creates a [TimeRangeAccessQueryPolicy] from a string representation of a time range
         * @param range the time range string in the format `[HH:mm, HH:mm)`, `(HH:mm, HH:mm)`, `[HH:mm, HH:mm]`, or `(HH:mm, HH:mm]`
         * @param allowInRange if true, the policy allows access within the specified range; if false, it denies access within the specified range
         */
        fun from(
            range: String,
            allowInRange: Boolean = true,
            clock: Clock = Clock.systemDefaultZone(),
        ): TimeRangeAccessQueryPolicy {
            val matcher = RANGE_PATTERN.matcher(range)
            require(matcher.matches()) { "Range must be in format '[HH:mm, HH:mm)' or similar" }

            val startBracket = matcher.group(1)
            val startTimeStr = matcher.group(2)
            val endTimeStr = matcher.group(3)
            val endBracket = matcher.group(4)

            return TimeRangeAccessQueryPolicy(
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
