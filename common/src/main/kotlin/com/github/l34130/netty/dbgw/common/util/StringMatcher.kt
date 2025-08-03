package com.github.l34130.netty.dbgw.common.util

abstract class StringMatcher<T>(
    protected val target: T,
) {
    abstract fun matches(value: String): Boolean

    abstract fun getPattern(): String

    companion object {
        val NONE: StringMatcher<*> = Any("<none>", false)
        val ANY: StringMatcher<*> = Any("", true)
        val ANY_PATTERN: StringMatcher<*> = Any(".*", true)

        fun create(target: String): StringMatcher<*> {
            if (target.isEmpty()) return ANY
            if (target == ".*") return ANY_PATTERN
            if (target == NONE.getPattern()) return NONE

            val branches = target.split("|")
            val matchers =
                branches
                    .map { branch ->
                        var branch = branch
                        var startsWith = false
                        var endsWith = false
                        var ignoreCase = false

                        if (branch.startsWith("(?i)")) {
                            ignoreCase = true
                            branch = branch.substring(4).lowercase()
                        }
                        if (branch.endsWith(".*")) {
                            endsWith = true
                            branch = branch.substring(0, branch.length - 2)
                        }
                        if (branch.startsWith(".*")) {
                            startsWith = true
                            branch = branch.substring(2)
                        }

                        var matcher: StringMatcher<*> =
                            if (startsWith && endsWith) {
                                Contains(branch)
                            } else if (startsWith) {
                                StartsWith(branch)
                            } else if (endsWith) {
                                EndsWith(branch)
                            } else {
                                Equals(branch)
                            }

                        if (ignoreCase) {
                            matcher = IgnoreCase(matcher)
                        }
                        matcher
                    }.toSet()

            return matchers.singleOrNull() ?: MatcherSet(matchers)
        }
    }

    private abstract class Simple(
        target: String,
    ) : StringMatcher<String>(target) {
        override fun getPattern(): String = target
    }

    private class Any(
        target: String,
        private val result: Boolean,
    ) : Simple(target) {
        override fun matches(value: String): Boolean = result
    }

    private class Pattern(
        target: String,
    ) : StringMatcher<Regex>(Regex(target)) {
        override fun matches(value: String): Boolean = target.matches(value)

        override fun getPattern(): String = target.pattern
    }

    private class Equals(
        target: String,
    ) : Simple(target) {
        override fun matches(value: String): Boolean = target == value
    }

    private class StartsWith(
        target: String,
    ) : Simple(target) {
        override fun matches(value: String): Boolean = value.startsWith(target)

        override fun getPattern(): String = "$target.*"
    }

    private class EndsWith(
        target: String,
    ) : Simple(target) {
        override fun matches(value: String): Boolean = value.endsWith(target)

        override fun getPattern(): String = ".*$target"
    }

    private class Contains(
        target: String,
    ) : Simple(target) {
        override fun matches(value: String): Boolean = value.contains(target)

        override fun getPattern(): String = ".*$target.*"
    }

    private class IgnoreCase(
        target: StringMatcher<*>,
    ) : StringMatcher<StringMatcher<*>>(target) {
        override fun matches(value: String): Boolean = target.matches(value.lowercase())

        override fun getPattern(): String = "(?i)${target.getPattern()}"
    }

    class MatcherSet(
        private val matchers: Set<StringMatcher<*>>,
    ) : StringMatcher<Set<StringMatcher<*>>>(matchers) {
        private val pattern: String by lazy {
            matchers.joinToString(separator = "|") { it.getPattern() }
        }

        override fun matches(value: String): Boolean = matchers.any { it.matches(value) }

        override fun getPattern(): String = pattern
    }
}
