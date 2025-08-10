package com.github.l34130.netty.dbgw.protocol.mysql.connection

import com.github.l34130.netty.dbgw.common.util.ByteUtils
import java.security.MessageDigest

// github.com/mysql/mysql-server/blob/b79ac1111737174c1b36ab5f63275f0191c000dc/libmysql/authentication_native_password/mysql_native_password.cc
object MySqlNativePasswordEncoder {
    fun encode(
        publicSeed: ByteArray,
        password: String,
    ): ByteArray {
        val sha1 = MessageDigest.getInstance("SHA-1")

        val first = sha1.digest(password.toByteArray())
        val second = sha1.digest(first)

        val combined =
            ByteArray(40).apply {
                publicSeed.copyInto(this, 0, 0, 20)
                second.copyInto(this, 20, 0, 20)
            }

        val xorBytes = sha1.digest(combined)
        return ByteUtils.xor(first, xorBytes)
    }
}
