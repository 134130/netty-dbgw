package com.github.l34130.netty.dbgw.policy.api.config

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
/**
 * Annotation to mark a class as a policy.
 */
annotation class Resource(
    /**
     * The group of the policy. This is used to categorize policies.
     * Must be a lowercase RFC 1123 subdomain.
     * @see [ResourceInfo.group]
     */
    val group: String,
    /**
     * The version of the policy. This is used to manage different versions of policies.
     * Must be a lowercase RFC 1123 label.
     * @see [ResourceInfo.version]
     */
    val version: String,
    /**
     * Kind of the policy resource. This is used to identify the type of policy in user-facing APIs.
     * Must be in PascalCase and singular.
     * @see [ResourceInfo.Names.kind]
     */
    val kind: String,
    /**
     * Plural name of the policy resource. This is used to identify the type of policy in a system.
     * Must be all lowercase.
     * @see [ResourceInfo.Names.plural]
     */
    val plural: String,
    /**
     * Singular name of the policy resource. This is used to identify the type of policy in a system.
     * Must be all lowercase. Defaults to the lowercase of [kind].
     * @see [ResourceInfo.Names.singular]
     */
    val singular: String = "",
)
