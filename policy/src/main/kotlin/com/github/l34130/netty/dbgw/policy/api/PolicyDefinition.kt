package com.github.l34130.netty.dbgw.policy.api

import com.github.l34130.netty.dbgw.common.util.ValidationUtil

class PolicyDefinition(
    /**
     * Group of the policy. Must be a valid lowercase RFC 1123 subdomain.
     */
    val group: String,
    /**
     * Version of the policy. Must be a valid lowercase RFC 1123 label.
     */
    val version: String,
    /**
     * Names of the policy resource.
     */
    val names: Names,
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
            }

        if (violations.isNotEmpty()) {
            throw IllegalArgumentException("Invalid PolicyMetadata: ${violations.joinToString(", ")}")
        }
    }

    fun key(): String = "${names.plural}.$version.$group"

    fun isApplicable(
        group: String,
        version: String,
        kind: String,
    ): Boolean = this.group == group && this.version == version && this.names.kind == kind

    override fun toString(): String = "PolicyMetadata(group='$group', version='$version', names=$names)"

    companion object {
        fun from(policyAnnotation: Policy): PolicyDefinition =
            PolicyDefinition(
                group = policyAnnotation.group,
                version = policyAnnotation.version,
                names =
                    if (policyAnnotation.singular.isEmpty()) {
                        Names(
                            kind = policyAnnotation.kind,
                            plural = policyAnnotation.plural,
                        )
                    } else {
                        Names(
                            kind = policyAnnotation.kind,
                            plural = policyAnnotation.plural,
                            singular = policyAnnotation.singular,
                        )
                    },
            )
    }

    class Names(
        /**
         * Serialized name of the resource. Must be in PascalCase and singular.
         */
        val kind: String,
        /**
         * Plural name of the resource to serve.
         * Must be all lowercase
         */
        val plural: String,
        /**
         * Singular name of the resource, It must be all lowercase. Defaults to the lowercase of [kind].
         */
        val singular: String = kind.lowercase(),
    ) {
        init {
            val violations =
                buildList {
                    ValidationUtil.validatePascalCase(kind).takeIf { it.isNotEmpty() }?.let {
                        add("kind must be a pascal case: $it")
                    }
                    ValidationUtil.validateRFC1123DNSLabel(plural).takeIf { it.isNotEmpty() }?.let {
                        add("plural must be a valid lowercase RFC 1123 label: $it")
                    }
                    ValidationUtil.validateRFC1123DNSLabel(singular).takeIf { it.isNotEmpty() }?.let {
                        add("singular must be a valid lowercase RFC 1123 label: $it")
                    }
                }

            if (violations.isNotEmpty()) {
                throw IllegalArgumentException("Invalid PolicyMetadata.Names: ${violations.joinToString(", ")}")
            }
        }

        override fun toString(): String = "PolicyMetadata.Names(kind='$kind', plural='$plural', singular='$singular')"
    }
}
