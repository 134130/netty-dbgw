package com.github.l34130.netty.dbgw.policy.builtin.database.query

import com.github.l34130.netty.dbgw.policy.api.database.DatabasePolicyInterceptor
import com.github.l34130.netty.dbgw.policy.api.database.query.DatabaseResultSetContext
import com.github.l34130.netty.dbgw.policy.builtin.util.IntRangeSet

class DatabaseResultSetMaskingPolicy(
    val maskingRegex: Regex,
) : DatabasePolicyInterceptor {
    override fun onResultSet(ctx: DatabaseResultSetContext) {
        ctx.addResultSetProcessor { columns ->
            columns.map { column ->
                val rangeSet =
                    maskingRegex
                        .findAll(column)
                        .fold(IntRangeSet()) { acc, matchResult ->
                            acc.add(matchResult.range)
                            acc
                        }

                buildString(column.length) {
                    for (char in column.indices) {
                        append(if (rangeSet.contains(char)) '*' else column[char])
                    }
                }
            }
        }
    }
}
