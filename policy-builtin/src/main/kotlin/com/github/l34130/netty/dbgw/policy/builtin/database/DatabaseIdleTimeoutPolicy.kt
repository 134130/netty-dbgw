package com.github.l34130.netty.dbgw.policy.builtin.database

import com.github.l34130.netty.dbgw.policy.api.Resource

@Resource(
    group = "builtin",
    version = "v1",
    kind = "DatabaseIdleTimeoutPolicy",
    plural = "databaseidletimeoutpolicies",
    singular = "databaseidletimeoutpolicy",
)
data class DatabaseIdleTimeoutPolicy(
    val timeoutSeconds: Int,
)
