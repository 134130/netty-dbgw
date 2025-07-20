package com.github.l34130.netty.dbgw.protocol.mysql

import com.github.l34130.netty.dbgw.protocol.mysql.constant.CapabilityFlag
import java.util.EnumSet

internal class Capabilities {
    private var clientCapabilities: EnumSet<CapabilityFlag>? = null
    private var serverCapabilities: EnumSet<CapabilityFlag>? = null
    private val interCapabilities: EnumSet<CapabilityFlag> by lazy {
        EnumSet.copyOf(serverCapabilities!!.intersect(clientCapabilities!!))
    }

    fun contains(capability: CapabilityFlag): Boolean = interCapabilities.contains(capability)

    fun setServerCapabilities(capabilities: EnumSet<CapabilityFlag>) {
        serverCapabilities = capabilities
    }

    fun setClientCapabilities(capabilities: EnumSet<CapabilityFlag>) {
        clientCapabilities = capabilities
    }

    fun enumSet(): EnumSet<CapabilityFlag> = interCapabilities

    override fun toString(): String = interCapabilities.toString()
}
