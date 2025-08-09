package com.github.l34130.netty.dbgw.policy.builtin.database

import com.github.l34130.netty.dbgw.common.database.FullyQualifiedName
import com.github.l34130.netty.dbgw.common.util.StringMatcher
import com.github.l34130.netty.dbgw.policy.api.PolicyDecision
import com.github.l34130.netty.dbgw.policy.api.PolicyDefinition
import com.github.l34130.netty.dbgw.policy.api.database.DatabasePolicy
import com.github.l34130.netty.dbgw.policy.api.database.query.DatabaseResultRowContext

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

    override fun onResultRow(ctx: DatabaseResultRowContext): PolicyDecision {
        ctx.columnDefinitions.forEachIndexed { index, columnDef ->
            // TODO: Remove column checking. The orgColumn should be used for matching.
            if (!columnMatcher.matches(columnDef.orgColumn.takeIf { it.isNotEmpty() } ?: columnDef.column)) {
                return@forEachIndexed
            }
            // TODO: Remove table checking. The orgTable should be used for matching.
            if (!tableMatcher.matches(columnDef.orgTable.takeIf { it.isNotEmpty() } ?: columnDef.table)) {
                return@forEachIndexed
            }
            if (!schemaMatcher.matches(columnDef.schema)) return@forEachIndexed
            if (!catalogMatcher.matches(columnDef.catalog)) return@forEachIndexed

            val columnData = ctx.resultRow[index] ?: return@forEachIndexed // Skip if column data is null

            // Check if the column data matches the filter regex
            val matches = dataMatcher.matches(columnData)
            if (matches) {
                return when (action) {
                    DatabaseRowLevelSecurityPolicyDefinition.Action.ALLOW ->
                        PolicyDecision.Allow(reason = "Row matches filter for fqn '$fqn' with regex '$filter'.")
                    DatabaseRowLevelSecurityPolicyDefinition.Action.DENY ->
                        PolicyDecision.Deny(reason = "Row does not match filter for fqn '$fqn' with regex '$filter'.")
                }
            }
        }

        // If no matching column found or any action taken, return NotApplicable
        return PolicyDecision.NotApplicable
    }
}
