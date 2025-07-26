package com.github.l34130.netty.dbgw.policy.api

import com.github.l34130.netty.dbgw.common.util.ValidationUtil

data class GroupVersionKind(
    /**
     * Group of the policy. Must be a valid lowercase RFC 1123 subdomain.
     */
    val group: String,
    /**
     * Version of the policy. Must be a valid lowercase RFC 1123 label.
     */
    val version: String,
    /**
     * Serialized name of the resource. Must be in PascalCase and singular.
     */
    val kind: String,
) {
    init {
        val violations =
            buildList {
                ValidationUtil.validateRFC1123DNSSubdomain(group).takeIf { it.isNotEmpty() }?.let {
                    add("group must be a valid lowercase RFC 1123 subdomain: $it")
                }
                ValidationUtil.validateRFC1123DNSLabel(version).takeIf { it.isNotEmpty() }?.let {
                    add("version must be a valid lowercase RFC 1123 label: $it")
                }
                ValidationUtil.validatePascalCase(kind).takeIf { it.isNotEmpty() }?.let {
                    add("kind must be a pascal case: $it")
                }
            }

        require(violations.isEmpty()) {
            "Invalid GroupVersionKind: ${violations.joinToString(", ")}"
        }
    }

    override fun toString(): String = "$group/$version, Kind=$kind"

    companion object {
        fun from(
            apiVersion: String,
            kind: String,
        ): GroupVersionKind {
            val parts = apiVersion.split('/')
            require(parts.size == 2) {
                "Invalid apiVersion format: $apiVersion. Expected format is '{group}/{version}'."
            }
            return GroupVersionKind(
                group = parts[0],
                version = parts[1],
                kind = kind,
            )
        }
    }
}
