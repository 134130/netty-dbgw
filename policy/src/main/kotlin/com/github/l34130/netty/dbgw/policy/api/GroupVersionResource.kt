package com.github.l34130.netty.dbgw.policy.api

import com.github.l34130.netty.dbgw.common.util.ValidationUtil

data class GroupVersionResource(
    val group: String,
    val version: String,
    val resource: String,
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
                ValidationUtil.validateRFC1123DNSLabel(resource).takeIf { it.isNotEmpty() }?.let {
                    add("resource must be a valid lowercase RFC 1123 label: $it")
                }
            }

        require(violations.isEmpty()) {
            "Invalid GroupVersionResource: ${violations.joinToString(", ")}"
        }
    }

    fun identifier(): String = "$resource.$group.$version"

    override fun toString(): String = "$group/$version, Resource=$resource"
}
