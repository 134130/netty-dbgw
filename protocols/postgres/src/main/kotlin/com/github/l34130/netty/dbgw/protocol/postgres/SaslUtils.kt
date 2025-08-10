package com.github.l34130.netty.dbgw.protocol.postgres

import com.github.l34130.netty.dbgw.common.util.ByteUtils
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object SaslUtils {
    fun decodeSaslScramAttributes(input: String): Map<String, String> {
        val result = mutableMapOf<String, String>()

        var index = 0
        val length = input.length
        while (index < length) {
            val keyEnd = input.indexOf('=', index)
            if (keyEnd < 0) break
            val key = input.substring(index, keyEnd)

            val valueEnd = input.indexOf(',', keyEnd + 1)
            val value =
                if (valueEnd < 0) {
                    input.substring(keyEnd + 1)
                } else {
                    input.substring(keyEnd + 1, valueEnd)
                }
            result[key] = value

            if (valueEnd < 0) break
            index = valueEnd + 1
        }

        return result
    }

    fun encodeSaslScramAttributes(attributes: Map<String, String>): String =
        attributes.entries.joinToString(",") { "${it.key}=${it.value}" }

    private val CLIENT_KEY = "Client Key".toByteArray()
    private val SERVER_KEY = "Server Key".toByteArray()

    fun generateScramClientProof(
        password: String,
        initialResponseAttrs: Map<String, String>,
        continueRequestAttrs: Map<String, String>,
    ): Pair<ByteArray, ByteArray> {
        val username =
            requireNotNull(initialResponseAttrs["n,,n"] ?: initialResponseAttrs["n"]) {
                "Missing 'n' attribute in initial response"
            }
        val clientNonce = requireNotNull(initialResponseAttrs["r"]) { "Missing 'r' attribute in initial response" }

        val combinedNonce = requireNotNull(continueRequestAttrs["r"]) { "Missing 'r' attribute in continue request" }
        val saltBase64 = requireNotNull(continueRequestAttrs["s"]) { "Missing 's' attribute in continue request" }
        val iterationsStr = requireNotNull(continueRequestAttrs["i"]) { "Missing 'i' attribute in continue request" }

        val serverNonce = combinedNonce.substring(clientNonce.length)
        val iterations = requireNotNull(iterationsStr.toIntOrNull()) { "Invalid 'i' attribute value: $iterationsStr" }

        val salt = Base64.getDecoder().decode(saltBase64)

        return generateScramClientProof(username, password, clientNonce, serverNonce, salt, iterations)
    }

    fun generateScramClientProof(
        username: String,
        password: String,
        clientNonce: String,
        serverNonce: String,
        salt: ByteArray,
        iterations: Int,
    ): Pair<ByteArray, ByteArray> {
        val combinedNonce = clientNonce + serverNonce

        val saltedPassword = pbkdf2HmacSha256(password.toCharArray(), salt, iterations)
        val clientKey = hmacSha256(saltedPassword, CLIENT_KEY)
        val storedKey = sha256(clientKey)

        val authMessage =
            run {
                val clientFirstMessageBare = "n=$username,r=$clientNonce"
                val serverFirstMessage = "r=$combinedNonce,s=${Base64.getEncoder().encodeToString(salt)},i=$iterations"
                val clientFinalMessageWithoutProof = "c=biws,r=$combinedNonce"
                "$clientFirstMessageBare,$serverFirstMessage,$clientFinalMessageWithoutProof"
            }

        val clientSignature = hmacSha256(storedKey, authMessage.toByteArray())
        val clientProof = ByteUtils.xor(clientKey, clientSignature)

        val serverKey = hmacSha256(saltedPassword, SERVER_KEY)
        val serverSignature = hmacSha256(serverKey, authMessage.toByteArray())

        return clientProof to serverSignature
    }

    private fun hmacSha256(
        key: ByteArray,
        data: ByteArray,
    ): ByteArray {
        val hmac = Mac.getInstance("HmacSHA256")
        val secretKeySpec = SecretKeySpec(key, "HmacSHA256")
        hmac.init(secretKeySpec)
        return hmac.doFinal(data)
    }

    private fun sha256(data: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(data)
    }

    private fun pbkdf2HmacSha256(
        password: CharArray,
        salt: ByteArray,
        iterations: Int,
    ): ByteArray {
        val spec =
            PBEKeySpec(
                password,
                salt,
                iterations,
                256,
            )
        val secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return secretKeyFactory.generateSecret(spec).encoded
    }
}
