package com.github.l34130.netty.dbgw.policy.builtin.database.query

import com.github.l34130.netty.dbgw.policy.api.database.DatabasePolicyInterceptor
import com.github.l34130.netty.dbgw.policy.api.database.query.DatabaseResultRowContext
import com.github.l34130.netty.dbgw.policy.builtin.util.IntRangeSet

class DatabaseResultSetMaskingPolicy(
    val maskingRegex: Regex,
) : DatabasePolicyInterceptor {
    override fun onResultRow(ctx: DatabaseResultRowContext) {
        ctx.addRowProcessorFactory { originalRow: List<String?> ->
            { row: Sequence<String?> ->
                val rangeSets: List<IntRangeSet?> =
                    originalRow
                        .map { column ->
                            if (column == null) {
                                return@map null
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

                    val rangeSet = rangeSets[index]!!

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
    }
}
