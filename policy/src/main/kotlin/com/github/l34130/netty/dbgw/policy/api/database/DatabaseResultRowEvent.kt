package com.github.l34130.netty.dbgw.policy.api.database

import com.github.l34130.netty.dbgw.common.database.ColumnDefinition

data class DatabaseResultRowEvent(
    val columnDefinitions: List<ColumnDefinition>,
    val resultRow: List<String?>,
)
