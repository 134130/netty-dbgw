package com.github.l34130.netty.dbgw.policy.builtin.database.query

import com.fasterxml.jackson.databind.JsonNode
import com.github.l34130.netty.dbgw.policy.api.ManifestMapper
import com.github.l34130.netty.dbgw.policy.api.ResourceFactory
import com.github.l34130.netty.dbgw.policy.api.convertValue
import kotlin.reflect.KClass

class DatabaseStatementTypePolicyFactory : ResourceFactory<DatabaseStatementTypePolicy> {
    override fun type(): KClass<DatabaseStatementTypePolicy> {
        return DatabaseStatementTypePolicy::class
    }

    override fun create(spec: JsonNode): DatabaseStatementTypePolicy {
        return ManifestMapper.convertValue(spec)
    }
}
