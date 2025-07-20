package com.github.l34130.netty.dbgw.protocol.postgres.startup

import com.github.l34130.netty.dbgw.core.GatewayState
import com.github.l34130.netty.dbgw.core.upstream
import com.github.l34130.netty.dbgw.protocol.postgres.PostgresGatewayState
import com.github.l34130.netty.dbgw.protocol.postgres.readUntilNull
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext

class StartupState : PostgresGatewayState {
    override fun onDownstreamMessage(
        ctx: ChannelHandlerContext,
        msg: ByteBuf,
    ): GatewayState<ByteBuf> {
        msg.markReaderIndex()

        val length = msg.readInt()
        check(msg.readableBytes() == length - 4) {
            "Invalid StartupMessage length: expected ${length - 4}, got ${msg.readableBytes()} bytes."
        }

        val majorVersion = msg.readShort()
        val minorVersion = msg.readShort()
        check(majorVersion == 3.toShort() || minorVersion == 0.toShort()) {
            "Unsupported PostgreSQL protocol version: $majorVersion.$minorVersion. " +
                "Only version 3.0 is supported."
        }

        logger.trace {
            "StartupMessage: majorVersion=$majorVersion, minorVersion=$minorVersion"
        }

        val user = msg.readUntilNull().toString(Charsets.UTF_8)
        val database = msg.readUntilNull().toString(Charsets.UTF_8)
        val options = msg.readUntilNull().toString(Charsets.UTF_8)
        val replication = msg.readUntilNull().toString(Charsets.UTF_8)

        val parameterValue = msg.readUntilNull().toString(Charsets.UTF_8)

        logger.trace {
            "StartupMessage: user='$user', database='$database', options='$options', replication='$replication', parameterValue='$parameterValue'"
        }

        msg.resetReaderIndex()
        ctx.upstream().writeAndFlush(msg)
        return this
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}
