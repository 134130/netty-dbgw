package com.github.l34130.netty.dbgw.core.policy

import com.github.l34130.netty.dbgw.policy.api.PolicyDefinition

interface PolicyChangeListener {
    fun onPolicyAdded(policy: PolicyDefinition)

    fun onPolicyRemoved(policy: PolicyDefinition)
}
