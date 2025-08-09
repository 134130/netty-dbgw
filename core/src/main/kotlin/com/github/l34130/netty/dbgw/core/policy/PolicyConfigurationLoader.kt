package com.github.l34130.netty.dbgw.core.policy

import com.github.l34130.netty.dbgw.policy.api.PolicyDefinition

interface PolicyConfigurationLoader {
    fun load(): List<PolicyDefinition>

    fun watchForChanges(listener: PolicyChangeListener): AutoCloseable

    companion object {
        val NOOP: PolicyConfigurationLoader =
            object : PolicyConfigurationLoader {
                override fun load(): List<PolicyDefinition> = emptyList()

                override fun watchForChanges(listener: PolicyChangeListener): AutoCloseable = AutoCloseable { /* No-op */ }
            }

        fun of(vararg definitions: PolicyDefinition): PolicyConfigurationLoader =
            object : PolicyConfigurationLoader {
                override fun load(): List<PolicyDefinition> = definitions.toList()

                override fun watchForChanges(listener: PolicyChangeListener): AutoCloseable = AutoCloseable { /* No-op */ }
            }
    }
}
