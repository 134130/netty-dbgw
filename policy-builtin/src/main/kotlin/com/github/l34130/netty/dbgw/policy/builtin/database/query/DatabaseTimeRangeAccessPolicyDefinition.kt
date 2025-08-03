package com.github.l34130.netty.dbgw.policy.builtin.database.query

import com.github.l34130.netty.dbgw.policy.api.PolicyDefinition
import com.github.l34130.netty.dbgw.policy.api.config.Resource
import com.github.l34130.netty.dbgw.policy.api.database.DatabasePolicyInterceptor
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
data class DatabaseTimeRangeAccessPolicyDefinition(
    private val startTime: LocalTime,
    private val endTime: LocalTime,
    private val startInclusive: Boolean,
    private val endInclusive: Boolean,
    private val action: Action = Action.ALLOW,
    private val clock: Clock,
) : PolicyDefinition {
    override fun createInterceptor(): DatabasePolicyInterceptor =
        DatabaseTimeRangeAccessPolicy(
            startTime = startTime,
            endTime = endTime,
            startInclusive = startInclusive,
            endInclusive = endInclusive,
            action = action,
            clock = clock,
        )

    init {
        require(startTime != endTime) {
            "Start and end time must not be the same"
        }
    }

    companion object {
        private val RANGE_PATTERN: Pattern =
            Pattern.compile("""^([(\[])\s*(\d{2}:\d{2})\s*,\s*(\d{2}:\d{2})\s*([)\]])$""")

        /**
         * Creates a [DatabaseTimeRangeAccessPolicyDefinition] from a string representation of a time range
         * @param range the time range string in the format `[HH:mm, HH:mm)`, `(HH:mm, HH:mm)`, `[HH:mm, HH:mm]`, or `(HH:mm, HH:mm]`
         * @param allowInRange if true, the policy allows access within the specified range; if false, it denies access within the specified range
         */
        fun from(
            range: String,
            action: Action = Action.ALLOW,
            clock: Clock = Clock.systemDefaultZone(),
        ): DatabaseTimeRangeAccessPolicyDefinition {
            val matcher = RANGE_PATTERN.matcher(range)
            require(matcher.matches()) { "Range must be in format '[HH:mm, HH:mm)' or similar" }

            val startBracket = matcher.group(1)
            val startTimeStr = matcher.group(2)
            val endTimeStr = matcher.group(3)
            val endBracket = matcher.group(4)

            return DatabaseTimeRangeAccessPolicyDefinition(
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
