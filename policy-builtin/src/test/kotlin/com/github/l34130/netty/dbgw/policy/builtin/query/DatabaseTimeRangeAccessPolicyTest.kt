package com.github.l34130.netty.dbgw.policy.builtin.query

import com.github.l34130.netty.dbgw.policy.api.ClientInfo
import com.github.l34130.netty.dbgw.policy.api.PolicyDecision
import com.github.l34130.netty.dbgw.policy.api.SessionInfo
import com.github.l34130.netty.dbgw.policy.api.database.DatabaseConnectionInfo
import com.github.l34130.netty.dbgw.policy.api.database.DatabaseContext
import com.github.l34130.netty.dbgw.policy.api.database.query.withQuery
import com.github.l34130.netty.dbgw.policy.builtin.database.query.DatabaseTimeRangeAccessPolicy
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlin.test.assertIs

class DatabaseTimeRangeAccessPolicyTest {
    private val ctx =
        DatabaseContext(
            clientInfo =
                ClientInfo(
                    sourceIps = listOf(),
                ),
            connectionInfo =
                DatabaseConnectionInfo(
                    databaseType = "",
                ),
            sessionInfo =
                SessionInfo(
                    sessionId = "",
                    userId = "",
                    username = "",
                ),
        ).withQuery("SELECT 1")

    private fun fixedClock(text: String): Clock = Clock.fixed(Instant.parse(text), ZoneId.of("UTC"))

    @TestFactory
    fun `test onQuery`() =
        listOf(
            // Case: [inclusive, exclusive) -> [09:00, 17:00)
            dynamicTest("range [inclusive, exclusive) at start time allows access") {
                val clock = fixedClock("2023-10-01T09:00:00Z")
                val policy =
                    DatabaseTimeRangeAccessPolicy.from(
                        range = "[09:00, 17:00)",
                        action = DatabaseTimeRangeAccessPolicy.Action.ALLOW,
                        clock = clock,
                    )
                val result = policy.onQuery(ctx)
                assertIs<PolicyDecision.Allow>(result)
            },
            dynamicTest("range [inclusive, exclusive) at end time is not applicable") {
                val clock = fixedClock("2023-10-01T17:00:00Z")
                val policy =
                    DatabaseTimeRangeAccessPolicy.from(
                        range = "[09:00, 17:00)",
                        action = DatabaseTimeRangeAccessPolicy.Action.ALLOW,
                        clock = clock,
                    )
                val result = policy.onQuery(ctx)
                assertIs<PolicyDecision.NotApplicable>(result)
            },
            dynamicTest("inverted range [inclusive, exclusive) at start time denies access") {
                val clock = fixedClock("2023-10-01T09:00:00Z")
                val policy =
                    DatabaseTimeRangeAccessPolicy.from(
                        range = "[09:00, 17:00)",
                        action = DatabaseTimeRangeAccessPolicy.Action.DENY,
                        clock = clock,
                    )
                val result = policy.onQuery(ctx)
                assertIs<PolicyDecision.Deny>(result)
            },
            dynamicTest("overnight range [inclusive, exclusive) at start time allows access") {
                val clock = fixedClock("2023-10-01T22:00:00Z")
                val policy =
                    DatabaseTimeRangeAccessPolicy.from(
                        range = "[22:00, 02:00)",
                        action = DatabaseTimeRangeAccessPolicy.Action.ALLOW,
                        clock = clock,
                    )
                val result = policy.onQuery(ctx)
                assertIs<PolicyDecision.Allow>(result)
            },
            dynamicTest("overnight range [inclusive, exclusive) at end time is not applicable") {
                val clock = fixedClock("2023-10-02T02:00:00Z")
                val policy =
                    DatabaseTimeRangeAccessPolicy.from(
                        range = "[22:00, 02:00)",
                        action = DatabaseTimeRangeAccessPolicy.Action.ALLOW,
                        clock = clock,
                    )
                val result = policy.onQuery(ctx)
                assertIs<PolicyDecision.NotApplicable>(result)
            },
            // Case: (exclusive, exclusive) -> (09:00, 17:00)
            dynamicTest("range (exclusive, exclusive)at start time is not applicable") {
                val clock = fixedClock("2023-10-01T09:00:00Z")
                val policy =
                    DatabaseTimeRangeAccessPolicy.from(
                        range = "(09:00, 17:00)",
                        action = DatabaseTimeRangeAccessPolicy.Action.ALLOW,
                        clock = clock,
                    )
                val result = policy.onQuery(ctx)
                assertIs<PolicyDecision.NotApplicable>(result)
            },
            dynamicTest("range (exclusive, exclusive) allows access right after start time") {
                val clock = fixedClock("2023-10-01T09:00:01Z")
                val policy =
                    DatabaseTimeRangeAccessPolicy.from(
                        range = "(09:00, 17:00)",
                        action = DatabaseTimeRangeAccessPolicy.Action.ALLOW,
                        clock = clock,
                    )
                val result = policy.onQuery(ctx)
                assertIs<PolicyDecision.Allow>(result)
            },
            // Case: [inclusive, inclusive] -> [09:00, 17:00]
            dynamicTest("range [inclusive, inclusive] at end time allows access") {
                val clock = fixedClock("2023-10-01T17:00:00Z")
                val policy =
                    DatabaseTimeRangeAccessPolicy.from(
                        range = "[09:00, 17:00]",
                        action = DatabaseTimeRangeAccessPolicy.Action.ALLOW,
                        clock = clock,
                    )
                val result = policy.onQuery(ctx)
                assertIs<PolicyDecision.Allow>(result)
            },
            dynamicTest("range [inclusive, inclusive] right after end time is not applicable") {
                val clock = fixedClock("2023-10-01T17:00:01Z")
                val policy =
                    DatabaseTimeRangeAccessPolicy.from(
                        range = "[09:00, 17:00]",
                        action = DatabaseTimeRangeAccessPolicy.Action.ALLOW,
                        clock = clock,
                    )
                val result = policy.onQuery(ctx)
                assertIs<PolicyDecision.NotApplicable>(result)
            },
            // Case: (exclusive, inclusive] -> (09:00, 17:00]
            dynamicTest("range (exclusive, inclusive] at start time is not applicable") {
                val clock = fixedClock("2023-10-01T09:00:00Z")
                val policy =
                    DatabaseTimeRangeAccessPolicy.from(
                        range = "(09:00, 17:00]",
                        action = DatabaseTimeRangeAccessPolicy.Action.ALLOW,
                        clock = clock,
                    )
                val result = policy.onQuery(ctx)
                assertIs<PolicyDecision.NotApplicable>(result)
            },
            dynamicTest("range (exclusive, inclusive] at end time allows access") {
                val clock = fixedClock("2023-10-01T17:00:00Z")
                val policy =
                    DatabaseTimeRangeAccessPolicy.from(
                        range = "(09:00, 17:00]",
                        action = DatabaseTimeRangeAccessPolicy.Action.ALLOW,
                        clock = clock,
                    )
                val result = policy.onQuery(ctx)
                assertIs<PolicyDecision.Allow>(result)
            },
        )
}
