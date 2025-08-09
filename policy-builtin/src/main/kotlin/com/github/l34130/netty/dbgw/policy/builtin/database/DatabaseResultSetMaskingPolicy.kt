package com.github.l34130.netty.dbgw.policy.builtin.database

import com.github.l34130.netty.dbgw.common.database.ColumnDefinition
import com.github.l34130.netty.dbgw.policy.api.PolicyDecision
import com.github.l34130.netty.dbgw.policy.api.PolicyDefinition
import com.github.l34130.netty.dbgw.policy.api.database.DatabasePolicy
import com.github.l34130.netty.dbgw.policy.api.database.DatabaseResultRowPolicyContext
import com.github.l34130.netty.dbgw.policy.builtin.util.IntRangeSet

class DatabaseResultSetMaskingPolicy(
    private val definition: DatabaseResultSetMaskingPolicyDefinition,
    val maskingRegex: Regex,
) : DatabasePolicy {
    override fun definition(): PolicyDefinition = definition

    override fun onResultRow(ctx: DatabaseResultRowPolicyContext) {
        ctx.addRowProcessorFactory { columnDefinitions: List<ColumnDefinition>, originalRow: List<String?> ->
            { row: Sequence<String?> ->
                val rangeSets: List<IntRangeSet?> =
                    originalRow
                        .mapIndexed { index, column ->
                            if (column == null) {
                                return@mapIndexed null
                            }
                            if (columnDefinitions[index].columnType.targetClass != String::class) {
                                // If the column is not a String, we do not apply regex masking
                                return@mapIndexed null
                            }

                            maskingRegex
                                .findAll(column)
                                .fold(IntRangeSet()) { acc, matchResult ->
                                    acc.add(matchResult.range)
                                    acc
                                }
                        }

                row.mapIndexed { index, column ->
                    if (column == null) {
                        return@mapIndexed null
                    }
                    val rangeSet = rangeSets[index]
                    if (rangeSet == null) {
                        return@mapIndexed column // No masking needed
                    }

                    buildString {
                        column.forEachIndexed { index, char ->
                            if (!rangeSet.contains(index)) {
                                append(char)
                            } else {
                                append('*') // Masking character
                            }
                        }
                    }
                }
            }
        }

        ctx.decision = PolicyDecision.NotApplicable // Not applicable as we are modifying the result row in place
    }
}
