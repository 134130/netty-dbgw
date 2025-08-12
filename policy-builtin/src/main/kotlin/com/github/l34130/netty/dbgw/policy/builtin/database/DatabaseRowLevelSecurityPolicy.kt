package com.github.l34130.netty.dbgw.policy.builtin.database

import com.github.l34130.netty.dbgw.common.database.FullyQualifiedName
import com.github.l34130.netty.dbgw.common.util.StringMatcher
import com.github.l34130.netty.dbgw.policy.api.PolicyDecision
import com.github.l34130.netty.dbgw.policy.api.PolicyDefinition
import com.github.l34130.netty.dbgw.policy.api.database.DatabasePolicy
import com.github.l34130.netty.dbgw.policy.api.database.DatabaseResultRowPolicyContext

class DatabaseRowLevelSecurityPolicy(
    private val definition: DatabaseRowLevelSecurityPolicyDefinition,
    val fqn: FullyQualifiedName,
    val filter: String,
    val action: DatabaseRowLevelSecurityPolicyDefinition.Action,
) : DatabasePolicy {
    private val catalogMatcher: StringMatcher<*> = fqn.catalog?.let { StringMatcher.create(it) } ?: StringMatcher.ANY_PATTERN
    private val schemaMatcher: StringMatcher<*> = fqn.schema?.let { StringMatcher.create(it) } ?: StringMatcher.ANY_PATTERN
    private val tableMatcher: StringMatcher<*> = fqn.table?.let { StringMatcher.create(it) } ?: StringMatcher.ANY_PATTERN
    private val columnMatcher: StringMatcher<*> = fqn.column?.let { StringMatcher.create(it) } ?: StringMatcher.ANY_PATTERN
    private val dataMatcher: StringMatcher<*> = StringMatcher.create(filter)

    override fun definition(): PolicyDefinition = definition

    override fun onResultRow(ctx: DatabaseResultRowPolicyContext) {
        for ((index, columnDef) in ctx.columnDefinitions.withIndex()) {
            // TODO: Remove column checking. The orgColumn should be used for matching.
            if (!columnMatcher.matches(columnDef.orgColumns.single().takeIf { it.isNotEmpty() } ?: columnDef.column!!)) {
                continue
            }
            // TODO: Remove table checking. The orgTable should be used for matching.
            if (!tableMatcher.matches(columnDef.orgTables.single().takeIf { it.isNotEmpty() } ?: columnDef.table!!)) {
                continue
            }
            if (!schemaMatcher.matches(columnDef.schema)) continue
            if (!catalogMatcher.matches(columnDef.catalog)) continue

            val columnData = ctx.resultRow[index] ?: continue // Skip if column data is null

            // Check if the column data matches the filter regex
            val matches = dataMatcher.matches(columnData)
            if (matches) {
                ctx.decision =
                    when (action) {
                        DatabaseRowLevelSecurityPolicyDefinition.Action.ALLOW ->
                            PolicyDecision.Allow(reason = "Row matches filter for fqn '$fqn' with regex '$filter'.")

                        DatabaseRowLevelSecurityPolicyDefinition.Action.DENY ->
                            PolicyDecision.Deny(reason = "Row does not match filter for fqn '$fqn' with regex '$filter'.")
                    }
                return // Stop processing if a match is found
            }
        }

        // If no matching column found or any action taken, return NotApplicable
        ctx.decision = PolicyDecision.NotApplicable
    }
}
