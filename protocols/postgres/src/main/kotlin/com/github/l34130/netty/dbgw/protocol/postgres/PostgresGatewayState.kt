package com.github.l34130.netty.dbgw.protocol.postgres

import com.github.l34130.netty.dbgw.core.GatewayState
import io.netty.buffer.ByteBuf

internal interface PostgresGatewayState : GatewayState<ByteBuf>
