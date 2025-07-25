package com.github.l34130.netty.dbgw.common.util

import java.util.regex.Pattern

typealias Violation = String

object ValidationUtil {
    private const val ALPHA_NUMERIC = "a-z0-9"
    private const val DNS1123_LABEL_FMT = "[$ALPHA_NUMERIC]([-$ALPHA_NUMERIC]*[$ALPHA_NUMERIC])?"
    private const val DNS1123_LABEL_MAX_LENGTH = 63
    private const val DNS1123_SUBDOMAIN_FMT = "($DNS1123_LABEL_FMT)(\\.$DNS1123_LABEL_FMT)*"
    private const val DNS1123_SUBDOMAIN_MAX_LENGTH = 253

    private val DNS1123_LABEL_REGEX = Pattern.compile("^$DNS1123_LABEL_FMT$")
    private val DNS1123_SUBDOMAIN_REGEX = Pattern.compile("^$DNS1123_SUBDOMAIN_FMT$")

    fun validatePascalCase(value: String): List<Violation> {
        val violations = mutableListOf<Violation>()
        if (value.isEmpty()) {
            violations.add("must not be empty")
        }
        if (!value.all { it.isLetterOrDigit() || it == '_' }) {
            violations.add("must consist of letters, digits, or underscores")
        }
        if (value.first().isLowerCase() || value.first().isDigit()) {
            // TODO: Change with regex
            violations.add("must start and end with an uppercase")
        }
        return violations
    }

    fun validateRFC1123DNSLabel(value: String): List<Violation> {
        val violations = mutableListOf<Violation>()
        if (value.length > DNS1123_LABEL_MAX_LENGTH) {
            violations.add("must be no more than $DNS1123_LABEL_MAX_LENGTH characters")
        }
        if (!DNS1123_LABEL_REGEX.matcher(value).matches()) {
            violations.add(
                "a lowercase RFC 1123 label must consist of lower case alphanumeric characters or '-', and must start and end with an alphanumeric character",
            )
        }
        return violations
    }

    fun validateRFC1123DNSSubdomain(value: String): List<Violation> {
        val violations = mutableListOf<Violation>()
        if (value.length > DNS1123_SUBDOMAIN_MAX_LENGTH) {
            violations.add("must be no more than $DNS1123_SUBDOMAIN_MAX_LENGTH characters")
        }
        if (!DNS1123_SUBDOMAIN_REGEX.matcher(value).matches()) {
            violations.add(
                "a lowercase RFC 1123 subdomain must consist of lower case alphanumeric characters, '-' or '.', and must start and end with an alphanumeric character",
            )
        }
        return violations
    }
}
