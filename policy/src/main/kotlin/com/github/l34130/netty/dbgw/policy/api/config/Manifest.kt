package com.github.l34130.netty.dbgw.policy.api.config

import com.fasterxml.jackson.annotation.JsonIgnore

class Manifest {
    @JsonIgnore
    val group: String

    @JsonIgnore
    val version: String

    val apiVersion: String
        get() = "$group/$version"
    val kind: String
    val metadata: Map<String, String>
    val spec: Map<String, Any>

    constructor(
        apiVersion: String,
        kind: String,
        metadata: Map<String, String> = emptyMap(),
        spec: Map<String, Any> = emptyMap(),
    ) {
        val parts = apiVersion.split('/')
        require(parts.size == 2) {
            "Invalid apiVersion format: $apiVersion. Expected format is 'group/version'."
        }
        this.group = parts[0]
        this.version = parts[1]
        this.kind = kind
        this.metadata = metadata
        this.spec = spec
    }

    fun groupVersionKind(): GroupVersionKind = GroupVersionKind.from(apiVersion, kind)

    override fun toString(): String = "ResourceManifest(apiVersion='$apiVersion', kind='$kind', metadata=$metadata, spec=$spec)"
}
