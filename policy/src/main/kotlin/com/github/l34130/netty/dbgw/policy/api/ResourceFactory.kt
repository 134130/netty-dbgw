package com.github.l34130.netty.dbgw.policy.api

import kotlin.reflect.KClass

interface ResourceFactory<T : Any> {
    fun type(): KClass<T>

    fun isApplicable(gvk: GroupVersionKind): Boolean

    fun create(props: Map<String, Any>): T
}
