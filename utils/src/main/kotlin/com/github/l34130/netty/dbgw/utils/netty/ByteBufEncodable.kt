package com.github.l34130.netty.dbgw.utils.netty

import io.netty.buffer.ByteBuf

interface ByteBufEncodable {
    fun toByteBuf(): ByteBuf
}
