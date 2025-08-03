package com.github.l34130.netty.dbgw.policy.builtin.database.query

import com.github.l34130.netty.dbgw.policy.api.PolicyDecision
import com.github.l34130.netty.dbgw.policy.api.database.DatabaseAuthenticationEvent
import com.github.l34130.netty.dbgw.policy.api.database.DatabaseContext
import com.github.l34130.netty.dbgw.policy.api.database.DatabasePolicyInterceptor
import com.github.l34130.netty.dbgw.policy.api.database.query.DatabaseQueryContext
import java.time.Clock
import java.time.LocalTime

data class DatabaseTimeRangeAccessPolicy(
    private val startTime: LocalTime,
    private val endTime: LocalTime,
    private val startInclusive: Boolean,
    private val endInclusive: Boolean,
    private val action: DatabaseTimeRangeAccessPolicyDefinition.Action = DatabaseTimeRangeAccessPolicyDefinition.Action.ALLOW,
    private val clock: Clock,
) : DatabasePolicyInterceptor {
    private val rangeNotation: String =
        "${if (startInclusive) '[' else '('}$startTime, $endTime${if (endInclusive) ']' else ')'}"

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
            DatabaseTimeRangeAccessPolicyDefinition.Action.ALLOW ->
                PolicyDecision.Allow(
                    reason = "current time is within the allowed range $rangeNotation",
                )
            DatabaseTimeRangeAccessPolicyDefinition.Action.DENY ->
                PolicyDecision.Deny(
                    reason = "current time is within the denied range $rangeNotation",
                )
        }
    }
}
