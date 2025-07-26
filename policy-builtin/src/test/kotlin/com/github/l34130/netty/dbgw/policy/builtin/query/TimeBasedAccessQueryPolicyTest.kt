package com.github.l34130.netty.dbgw.policy.builtin.query

import com.github.l34130.netty.dbgw.policy.api.ClientInfo
import com.github.l34130.netty.dbgw.policy.api.DatabaseConnectionInfo
import com.github.l34130.netty.dbgw.policy.api.SessionInfo
import com.github.l34130.netty.dbgw.policy.api.query.DatabaseQueryPolicyContext
import com.github.l34130.netty.dbgw.policy.api.query.DatabaseQueryPolicyResult
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlin.test.assertIs

class TimeBasedAccessQueryPolicyTest {
    private val ctx =
        DatabaseQueryPolicyContext(
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
        )

    private fun fixedClock(text: String): Clock = Clock.fixed(Instant.parse(text), ZoneId.of("UTC"))

    @TestFactory
    fun `test evaluate`() =
        listOf(
            // Case: [inclusive, exclusive) -> [09:00, 17:00)
            dynamicTest("range [inclusive, exclusive) allows access at start time") {
                val clock = fixedClock("2023-10-01T09:00:00Z")
                val policy = DatabaseTimeRangeAccessQueryPolicy.from(range = "[09:00, 17:00)", allowInRange = true, clock = clock)
                val result = policy.evaluate(ctx, "SELECT 1")
                assertIs<DatabaseQueryPolicyResult.Allowed>(result)
            },
            dynamicTest("range [inclusive, exclusive) denies access at end time") {
                val clock = fixedClock("2023-10-01T17:00:00Z")
                val policy = DatabaseTimeRangeAccessQueryPolicy.from(range = "[09:00, 17:00)", allowInRange = true, clock = clock)
                val result = policy.evaluate(ctx, "SELECT 1")
                assertIs<DatabaseQueryPolicyResult.Denied>(result)
            },
            dynamicTest("inverted range [inclusive, exclusive) denies access at start time") {
                val clock = fixedClock("2023-10-01T09:00:00Z")
                val policy = DatabaseTimeRangeAccessQueryPolicy.from(range = "[09:00, 17:00)", allowInRange = false, clock = clock)
                val result = policy.evaluate(ctx, "SELECT 1")
                assertIs<DatabaseQueryPolicyResult.Denied>(result)
            },
            dynamicTest("overnight range [inclusive, exclusive) allows access at start time") {
                val clock = fixedClock("2023-10-01T22:00:00Z")
                val policy = DatabaseTimeRangeAccessQueryPolicy.from(range = "[22:00, 02:00)", allowInRange = true, clock = clock)
                val result = policy.evaluate(ctx, "SELECT 1")
                assertIs<DatabaseQueryPolicyResult.Allowed>(result)
            },
            dynamicTest("overnight range [inclusive, exclusive) denies access at end time") {
                val clock = fixedClock("2023-10-02T02:00:00Z")
                val policy = DatabaseTimeRangeAccessQueryPolicy.from(range = "[22:00, 02:00)", allowInRange = true, clock = clock)
                val result = policy.evaluate(ctx, "SELECT 1")
                assertIs<DatabaseQueryPolicyResult.Denied>(result)
            },
            // Case: (exclusive, exclusive) -> (09:00, 17:00)
            dynamicTest("range (exclusive, exclusive) denies access at start time") {
                val clock = fixedClock("2023-10-01T09:00:00Z")
                val policy = DatabaseTimeRangeAccessQueryPolicy.from(range = "(09:00, 17:00)", allowInRange = true, clock = clock)
                val result = policy.evaluate(ctx, "SELECT 1")
                assertIs<DatabaseQueryPolicyResult.Denied>(result)
            },
            dynamicTest("range (exclusive, exclusive) allows access right after start time") {
                val clock = fixedClock("2023-10-01T09:00:01Z")
                val policy = DatabaseTimeRangeAccessQueryPolicy.from(range = "(09:00, 17:00)", allowInRange = true, clock = clock)
                val result = policy.evaluate(ctx, "SELECT 1")
                assertIs<DatabaseQueryPolicyResult.Allowed>(result)
            },
            // Case: [inclusive, inclusive] -> [09:00, 17:00]
            dynamicTest("range [inclusive, inclusive] allows access at end time") {
                val clock = fixedClock("2023-10-01T17:00:00Z")
                val policy = DatabaseTimeRangeAccessQueryPolicy.from(range = "[09:00, 17:00]", allowInRange = true, clock = clock)
                val result = policy.evaluate(ctx, "SELECT 1")
                assertIs<DatabaseQueryPolicyResult.Allowed>(result)
            },
            dynamicTest("range [inclusive, inclusive] denies access right after end time") {
                val clock = fixedClock("2023-10-01T17:00:01Z")
                val policy = DatabaseTimeRangeAccessQueryPolicy.from(range = "[09:00, 17:00]", allowInRange = true, clock = clock)
                val result = policy.evaluate(ctx, "SELECT 1")
                assertIs<DatabaseQueryPolicyResult.Denied>(result)
            },
            // Case: (exclusive, inclusive] -> (09:00, 17:00]
            dynamicTest("range (exclusive, inclusive] denies access at start time") {
                val clock = fixedClock("2023-10-01T09:00:00Z")
                val policy = DatabaseTimeRangeAccessQueryPolicy.from(range = "(09:00, 17:00]", allowInRange = true, clock = clock)
                val result = policy.evaluate(ctx, "SELECT 1")
                assertIs<DatabaseQueryPolicyResult.Denied>(result)
            },
            dynamicTest("range (exclusive, inclusive] allows access at end time") {
                val clock = fixedClock("2023-10-01T17:00:00Z")
                val policy = DatabaseTimeRangeAccessQueryPolicy.from(range = "(09:00, 17:00]", allowInRange = true, clock = clock)
                val result = policy.evaluate(ctx, "SELECT 1")
                assertIs<DatabaseQueryPolicyResult.Allowed>(result)
            },
        )
}
