package com.github.l34130.netty.dbgw.protocol.mysql.connection

import java.security.MessageDigest

object CachingSha256PasswordEncoder {
    fun encode(
        nonce: ByteArray,
        password: String,
    ): ByteArray {
        val nonce = nonce.copyOf(20) // Ensure nonce is exactly 20 bytes long
        val sha256 = MessageDigest.getInstance("SHA-256")

        val digest1 = sha256.digest(password.toByteArray())
        val digest2 = sha256.digest(digest1)

        val combined = ByteArray(digest2.size + nonce.size)
        System.arraycopy(digest2, 0, combined, 0, digest2.size)
        System.arraycopy(nonce, 0, combined, digest2.size, nonce.size)

        val digest3 = sha256.digest(combined)

        return ByteArray(digest1.size) { i ->
            (digest1[i].toInt() xor digest3[i].toInt()).toByte()
        }
    }
}
