package com.github.l34130.netty.dbgw.policy.api

import com.fasterxml.jackson.databind.JsonNode
import kotlin.reflect.KClass

interface ResourceFactory<T : Any> {
    fun type(): KClass<T>

    fun create(spec: JsonNode): T
}
