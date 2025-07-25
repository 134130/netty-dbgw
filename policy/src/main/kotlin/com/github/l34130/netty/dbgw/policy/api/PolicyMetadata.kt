package com.github.l34130.netty.dbgw.policy.api

import com.github.l34130.netty.dbgw.common.util.ValidationUtil

class PolicyMetadata(
    val group: String,
    val version: String,
    val kind: String,
) {
    init {
        val violations =
            buildList {
                ValidationUtil.validateRFC1123DnsSubdomain(group).takeIf { it.isNotEmpty() }?.let {
                    add("group must be a valid lowercase RFC 1123 subdomain: $it")
                }
                ValidationUtil.validateRFC1123DnsLabel(version).takeIf { it.isNotEmpty() }?.let {
                    add("version must be a valid lowercase RFC 1123 label: $it")
                }
                ValidationUtil.validatePascalCase(kind).takeIf { it.isNotEmpty() }?.let {
                    add("kind must be a pascal case: $it")
                }
            }

        if (violations.isNotEmpty()) {
            throw IllegalArgumentException("Invalid PolicyMetadata: ${violations.joinToString(", ")}")
        }
    }

    fun key(): String = "$group/$version/$kind"

    override fun toString(): String = "PolicyMetadata(group='$group', version='$version', kind='$kind')"
}
