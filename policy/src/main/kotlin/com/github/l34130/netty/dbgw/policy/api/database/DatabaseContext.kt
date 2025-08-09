package com.github.l34130.netty.dbgw.policy.api.database

import com.github.l34130.netty.dbgw.policy.api.ClientInfo
import com.github.l34130.netty.dbgw.policy.api.SessionInfo

// TODO: jq able
open class DatabaseContext(
    var clientInfo: ClientInfo,
    var connectionInfo: DatabaseConnectionInfo,
    var sessionInfo: SessionInfo,
    var attributes: MutableMap<String, Any> = mutableMapOf(),
) {
    protected constructor(ctx: DatabaseContext) : this(
        clientInfo = ctx.clientInfo,
        connectionInfo = ctx.connectionInfo,
        sessionInfo = ctx.sessionInfo,
        attributes = ctx.attributes,
    )
}
