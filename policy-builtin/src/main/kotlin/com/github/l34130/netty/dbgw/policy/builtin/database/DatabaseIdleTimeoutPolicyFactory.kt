package com.github.l34130.netty.dbgw.policy.builtin.database

import com.fasterxml.jackson.databind.JsonNode
import com.github.l34130.netty.dbgw.policy.api.ManifestMapper
import com.github.l34130.netty.dbgw.policy.api.ResourceFactory
import com.github.l34130.netty.dbgw.policy.api.convertValue
import kotlin.reflect.KClass

class DatabaseIdleTimeoutPolicyFactory : ResourceFactory<DatabaseIdleTimeoutPolicy> {
    override fun type(): KClass<DatabaseIdleTimeoutPolicy> {
        return DatabaseIdleTimeoutPolicy::class
    }

    override fun create(spec: JsonNode): DatabaseIdleTimeoutPolicy {
        return ManifestMapper.convertValue(spec)
    }
}
