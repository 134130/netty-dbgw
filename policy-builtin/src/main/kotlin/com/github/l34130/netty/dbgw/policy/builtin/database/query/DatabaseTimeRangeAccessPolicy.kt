package com.github.l34130.netty.dbgw.policy.builtin.database.query

import com.github.l34130.netty.dbgw.policy.api.PolicyDecision
import com.github.l34130.netty.dbgw.policy.api.config.Resource
import com.github.l34130.netty.dbgw.policy.api.database.DatabaseAuthenticationEvent
import com.github.l34130.netty.dbgw.policy.api.database.DatabaseContext
import com.github.l34130.netty.dbgw.policy.api.database.DatabasePolicyInterceptor
import com.github.l34130.netty.dbgw.policy.api.database.query.DatabaseQueryContext
import java.time.Clock
import java.time.LocalTime
import java.util.regex.Pattern

@Resource(
    group = "builtin",
    version = "v1",
    kind = "DatabaseTimeRangeAccessPolicy",
    plural = "databasetimerangeaccesspolicies",
    singular = "databasetimerangeaccesspolicy",
)
data class DatabaseTimeRangeAccessPolicy(
    private val startTime: LocalTime,
    private val endTime: LocalTime,
    private val startInclusive: Boolean,
    private val endInclusive: Boolean,
    private val action: Action = Action.ALLOW,
    private val clock: Clock,
) : DatabasePolicyInterceptor {
    private val rangeNotation: String =
        "${if (startInclusive) '[' else '('}$startTime, $endTime${if (endInclusive) ']' else ')'}"

    init {
        require(startTime != endTime) {
            "Start and end time must not be the same"
        }
    }

    override fun onAuthentication(
        ctx: DatabaseContext,
        evt: DatabaseAuthenticationEvent,
    ): PolicyDecision = evaluate()

    override fun onQuery(ctx: DatabaseQueryContext): PolicyDecision = evaluate()

    private fun evaluate(): PolicyDecision {
        val currentTime = LocalTime.now(clock)

        val afterStart = if (startInclusive) !currentTime.isBefore(startTime) else currentTime.isAfter(startTime)
        val beforeEnd = if (endInclusive) !currentTime.isAfter(endTime) else currentTime.isBefore(endTime)

        val isWithinRange =
            if (startTime.isBefore(endTime)) {
                // 09:00 ~ 17:00
                afterStart && beforeEnd
            } else {
                // 22:00 ~ 02:00 (crossing midnight)
                !(beforeEnd && afterStart)
            }

        if (!isWithinRange) return PolicyDecision.NotApplicable

        return when (action) {
            Action.ALLOW ->
                PolicyDecision.Allow(
                    reason = "current time is within the allowed range $rangeNotation",
                )
            Action.DENY ->
                PolicyDecision.Deny(
                    reason = "current time is within the denied range $rangeNotation",
                )
        }
    }

    companion object {
        private val RANGE_PATTERN: Pattern =
            Pattern.compile("""^([(\[])\s*(\d{2}:\d{2})\s*,\s*(\d{2}:\d{2})\s*([)\]])$""")

        /**
         * Creates a [DatabaseTimeRangeAccessPolicy] from a string representation of a time range
         * @param range the time range string in the format `[HH:mm, HH:mm)`, `(HH:mm, HH:mm)`, `[HH:mm, HH:mm]`, or `(HH:mm, HH:mm]`
         * @param allowInRange if true, the policy allows access within the specified range; if false, it denies access within the specified range
         */
        fun from(
            range: String,
            action: Action = Action.ALLOW,
            clock: Clock = Clock.systemDefaultZone(),
        ): DatabaseTimeRangeAccessPolicy {
            val matcher = RANGE_PATTERN.matcher(range)
            require(matcher.matches()) { "Range must be in format '[HH:mm, HH:mm)' or similar" }

            val startBracket = matcher.group(1)
            val startTimeStr = matcher.group(2)
            val endTimeStr = matcher.group(3)
            val endBracket = matcher.group(4)

            return DatabaseTimeRangeAccessPolicy(
                startTime = LocalTime.parse(startTimeStr),
                endTime = LocalTime.parse(endTimeStr),
                startInclusive = startBracket == "[",
                endInclusive = endBracket == "]",
                action = action,
                clock = clock,
            )
        }
    }

    enum class Action { ALLOW, DENY }
}
