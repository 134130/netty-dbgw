@file:Suppress("ktlint:standard:filename")

package com.github.l34130.netty.dbgw.test

import com.github.l34130.netty.dbgw.core.policy.PolicyChangeListener
import com.github.l34130.netty.dbgw.core.policy.PolicyConfigurationLoader
import com.github.l34130.netty.dbgw.policy.api.PolicyDefinition

val PolicyConfigurationLoader.Companion.ALLOW_ALL: PolicyConfigurationLoader by lazy {
    object : PolicyConfigurationLoader {
        override fun load(): List<PolicyDefinition> = listOf(PolicyDefinition.ALLOW_ALL)

        override fun watchForChanges(listener: PolicyChangeListener): AutoCloseable =
            AutoCloseable {
                // No-op for this test
            }
    }
}
