package com.github.l34130.netty.dbgw.policy.api.database

import com.github.l34130.netty.dbgw.common.database.ColumnDefinition
import com.github.l34130.netty.dbgw.policy.api.PolicyDecision

open class DatabasePolicyContext protected constructor(
    var decision: PolicyDecision,
    databaseCtx: DatabaseContext,
) : DatabaseContext(databaseCtx) {
    protected constructor(ctx: DatabasePolicyContext) : this(
        decision = ctx.decision,
        databaseCtx = ctx,
    )

    companion object {
        fun DatabaseContext.toPolicyContext(): DatabasePolicyContext =
            DatabasePolicyContext(
                decision = PolicyDecision.NotApplicable,
                databaseCtx = this,
            )
    }
}

class DatabaseAuthenticationPolicyContext private constructor(
    var username: String,
    var password: String? = null,
    policyCtx: DatabasePolicyContext,
) : DatabasePolicyContext(policyCtx) {
    companion object {
        fun DatabasePolicyContext.toAuthenticationPolicyContext(username: String): DatabaseAuthenticationPolicyContext =
            DatabaseAuthenticationPolicyContext(
                username = username,
                policyCtx = this,
            )
    }
}

class DatabaseQueryPolicyContext private constructor(
    val query: String,
    policyCtx: DatabasePolicyContext,
) : DatabasePolicyContext(policyCtx) {
    companion object {
        fun DatabasePolicyContext.toQueryPolicyContext(query: String): DatabaseQueryPolicyContext =
            DatabaseQueryPolicyContext(
                query = query,
                policyCtx = this,
            )
    }
}

class DatabaseResultRowPolicyContext private constructor(
    val columnDefinitions: List<ColumnDefinition>,
    val resultRow: List<String?>,
    policyCtx: DatabasePolicyContext,
) : DatabasePolicyContext(policyCtx) {
    private val processors = mutableListOf<(Sequence<String?>) -> Sequence<String?>>()

    fun resultRow(): List<String?> =
        processors
            .fold(resultRow.asSequence()) { acc, processor ->
                processor(acc)
            }.toList()

    fun addRowProcessorFactory(
        processorFactory: (
            columnDefinitions: List<ColumnDefinition>,
            originalRow: List<String?>,
        ) -> (Sequence<String?>) -> Sequence<String?>,
    ) {
        processors.add(processorFactory(columnDefinitions, resultRow))
    }

    companion object {
        fun DatabasePolicyContext.toResultRowPolicyContext(
            columnDefinitions: List<ColumnDefinition>,
            resultRow: List<String?>,
        ): DatabaseResultRowPolicyContext =
            DatabaseResultRowPolicyContext(
                columnDefinitions = columnDefinitions,
                resultRow = resultRow,
                policyCtx = this,
            )
    }
}
