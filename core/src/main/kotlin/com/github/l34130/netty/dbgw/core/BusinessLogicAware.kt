package com.github.l34130.netty.dbgw.core

/**
 * Marker interface for [GatewayState] or [MessageInterceptor] which contains business logic, so that it
 * runs in a separate pool of business threads to avoid blocking Netty's EventLoop threads.
 */
interface BusinessLogicAware
