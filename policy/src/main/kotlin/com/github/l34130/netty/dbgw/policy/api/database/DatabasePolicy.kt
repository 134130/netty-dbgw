package com.github.l34130.netty.dbgw.policy.api.database

import com.github.l34130.netty.dbgw.policy.api.PolicyDefinition

interface DatabasePolicy : DatabaseInterceptor {
    fun definition(): PolicyDefinition
}
