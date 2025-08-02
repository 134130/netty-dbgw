package com.github.l34130.netty.dbgw.policy.builtin.database.query

import com.fasterxml.jackson.databind.JsonNode
import com.github.l34130.netty.dbgw.policy.api.ManifestMapper
import com.github.l34130.netty.dbgw.policy.api.ResourceFactory
import com.github.l34130.netty.dbgw.policy.api.convertValue
import kotlin.reflect.KClass

class DatabaseTimeRangeAccessQueryPolicyFactory :
    ResourceFactory<DatabaseTimeRangeAccessQueryPolicy> {
    override fun type(): KClass<DatabaseTimeRangeAccessQueryPolicy> {
        return DatabaseTimeRangeAccessQueryPolicy::class
    }

    override fun create(spec: JsonNode): DatabaseTimeRangeAccessQueryPolicy {
        return ManifestMapper.convertValue(spec)
    }
}
