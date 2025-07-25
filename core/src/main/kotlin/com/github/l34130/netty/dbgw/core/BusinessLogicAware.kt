package com.github.l34130.netty.dbgw.core

/**
 * The [DatabaseGatewayState] or [MessageInterceptor] that implements this interface contains business logic, so that it
 * runs in a separate pool of business threads to avoid blocking Netty's EventLoop threads.
 */
interface BusinessLogicAware
