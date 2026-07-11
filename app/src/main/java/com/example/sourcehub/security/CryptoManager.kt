package com.example.sourcehub.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class CryptoManager(context: Context) {

    private val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }

    init {
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            generateKey()
        }
    }

    private fun generateKey() {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        keyGenerator.generateKey()
    }

    private fun getSecretKey(): SecretKey {
        return (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
    }

    fun encryptFile(inputFile: File, outputFile: File) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())

        FileInputStream(inputFile).use { input ->
            FileOutputStream(outputFile).use { output ->
                // Write IV first
                output.write(cipher.iv)
                CipherOutputStream(output, cipher).use { cos ->
                    input.copyTo(cos, DEFAULT_BUFFER_SIZE)
                }
            }
        }
    }

    fun decryptFile(inputFile: File, outputFile: File) {
        FileInputStream(inputFile).use { input ->
            // Read IV
            val iv = ByteArray(GCM_IV_LENGTH)
            input.read(iv)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)

            FileOutputStream(outputFile).use { output ->
                CipherInputStream(input, cipher).use { cis ->
                    cis.copyTo(output, DEFAULT_BUFFER_SIZE)
                }
            }
        }
    }

    fun encryptStream(inputStream: InputStream, outputStream: OutputStream) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        outputStream.write(cipher.iv)
        CipherOutputStream(outputStream, cipher).use { cos ->
            inputStream.copyTo(cos, DEFAULT_BUFFER_SIZE)
        }
    }

    fun decryptStream(inputStream: InputStream, outputStream: OutputStream) {
        val iv = ByteArray(GCM_IV_LENGTH)
        inputStream.read(iv)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)

        CipherInputStream(inputStream, cipher).use { cis ->
            cis.copyTo(outputStream, DEFAULT_BUFFER_SIZE)
        }
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "sourcehub_file_encryption_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128
        private const val DEFAULT_BUFFER_SIZE = 8192
    }
}
