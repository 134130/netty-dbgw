package com.github.l34130.netty.dbgw.policy.builtin.database

import com.github.l34130.netty.dbgw.common.util.StringMatcher
import com.github.l34130.netty.dbgw.policy.api.PolicyDecision
import com.github.l34130.netty.dbgw.policy.api.PolicyDefinition
import com.github.l34130.netty.dbgw.policy.api.database.DatabasePolicy
import com.github.l34130.netty.dbgw.policy.api.database.query.DatabaseResultRowContext

class DatabaseRowLevelSecurityPolicy(
    private val definition: DatabaseRowLevelSecurityPolicyDefinition,
    val database: String?,
    val schema: String?,
    val table: String?,
    val column: String,
    val filterRegex: String,
    val action: DatabaseRowLevelSecurityPolicyDefinition.Action,
) : DatabasePolicy {
    override fun definition(): PolicyDefinition = definition

    private val columnMatcher: StringMatcher<*> = StringMatcher.create(column)
    private val dataMatcher: StringMatcher<*> = StringMatcher.create(filterRegex)

    override fun onResultRow(ctx: DatabaseResultRowContext): PolicyDecision {
        ctx.columnDefinitions.forEachIndexed { index, columnDef ->
            if (!columnMatcher.matches(columnDef.orgColumn)) {
                return@forEachIndexed
            }

            val columnData = ctx.resultRow[index] ?: return@forEachIndexed // Skip if column data is null

            // Check if the column data matches the filter regex
            val matches = dataMatcher.matches(columnData)
            if (matches) {
                return when (action) {
                    DatabaseRowLevelSecurityPolicyDefinition.Action.ALLOW ->
                        PolicyDecision.Allow(reason = "Row matches filter for column '$column' with regex '$filterRegex'.")
                    DatabaseRowLevelSecurityPolicyDefinition.Action.DENY ->
                        PolicyDecision.Deny(reason = "Row does not match filter for column '$column' with regex '$filterRegex'.")
                }
            }
        }

        // If no matching column found or any action taken, return NotApplicable
        return PolicyDecision.NotApplicable
    }
}
