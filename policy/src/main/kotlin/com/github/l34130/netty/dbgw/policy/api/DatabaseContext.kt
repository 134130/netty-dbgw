package com.github.l34130.netty.dbgw.policy.api

// TODO: jq able
open class DatabaseContext(
    var clientInfo: ClientInfo,
    var connectionInfo: DatabaseConnectionInfo,
    var sessionInfo: SessionInfo,
    var attributes: MutableMap<String, Any> = mutableMapOf(),
) {
    companion object
}
