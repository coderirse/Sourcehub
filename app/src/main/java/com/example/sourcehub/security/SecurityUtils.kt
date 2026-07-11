package com.example.sourcehub.security

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object SecurityUtils {

    private const val HMAC_SECRET = "sourcehub_mock_secret_key_2024" // In production: store in native code
    private const val ALGORITHM = "HmacSHA256"

    fun generateRequestSignature(
        method: String,
        path: String,
        timestamp: Long,
        body: String = ""
    ): String {
        val data = "$method|$path|$timestamp|$body"
        val secretKeySpec = SecretKeySpec(HMAC_SECRET.toByteArray(Charsets.UTF_8), ALGORITHM)
        val mac = Mac.getInstance(ALGORITHM)
        mac.init(secretKeySpec)
        val hash = mac.doFinal(data.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }

    fun generateNonce(length: Int = 16): String {
        val random = SecureRandom()
        val bytes = ByteArray(length)
        random.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }

    fun generateUuid(): String {
        return java.util.UUID.randomUUID().toString()
    }
}
