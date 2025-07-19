package com.github.l34130.netty.dbgw.app.handler.codec.mysql

import com.github.l34130.netty.dbgw.app.handler.codec.mysql.connection.HandshakeState
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.channel.ChannelHandlerContext

class GatewayStateMachine {
    private var currentState: GatewayState = HandshakeState()

    fun processDownstream(
        ctx: ChannelHandlerContext,
        packet: Packet,
    ) {
        logger.debug { "Processing downstream packet. currentState='${currentState::class.simpleName}'" }
        val nextState = currentState.onDownstreamPacket(ctx, packet)
        logger.debug {
            "${currentState::class.simpleName} -> ${nextState.javaClass.simpleName}"
        }
        currentState = nextState
    }

    fun processUpstream(
        ctx: ChannelHandlerContext,
        packet: Packet,
    ) {
        logger.debug { "Processing upstream packet. currentState='${currentState::class.simpleName}'" }
        val nextState = currentState.onUpstreamPacket(ctx, packet)
        logger.debug {
            "${currentState::class.simpleName} -> ${nextState.javaClass.simpleName}"
        }
        currentState = nextState
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
