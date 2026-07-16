/**
 * 基于 Android 密钥库的 AES-256-GCM 文件加密与解密。
 *
 * 此类为文件和流提供对称加密——主要用于保护下载的数字产品
 * （PDF、文档）在磁盘上离线存储时的安全。
 *
 * ## 安全特性
 * - **密钥材料**永不离开 Android 密钥库（在支持的设备上由硬件支持）。
 *   应用仅持有密钥别名的引用。
 * - **认证加密**：AES-GCM 同时提供机密性和完整性。
 *   任何对密文的篡改都会导致解密失败并抛出 AEADBadTagException。
 * - **每次加密使用唯一 IV**：Android 的 [Cipher] 在每次以 ENCRYPT_MODE 调用
 *   [Cipher.init] 时生成随机的 12 字节 IV。IV 被前置到密文中，以便解密时可用。
 *
 * ## 文件格式
 * ```
 * [ 12字节 IV ][ 密文（包含128位 GCM 认证标签） ]
 * ```
 */
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

/**
 * 使用密钥库支持的 AES-256-GCM 密钥管理文件级加密。
 *
 * 密钥仅生成一次（首次访问时），并在所有后续加密/解密操作中重用。
 * 密钥别名为 [KEY_ALIAS]。
 *
 * @param context [KeyStore] 初始化密钥库所需；仅在构造函数中用于加载 AndroidKeyStore 提供者。
 */
class CryptoManager(context: Context) {

    /**
     * Android 密钥库实例。急切加载，以便密钥生成失败
     * （例如在 TEE 损坏的设备上）在构造时被捕获，而非在后续加密调用中才发现。
     */
    private val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }

    init {
        // 仅生成一次 AES 密钥。后续应用启动将通过 [keyStore.containsAlias]
        // 找到已有密钥并跳过生成。
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            generateKey()
        }
    }

    /**
     * 在 Android 密钥库内创建新的 AES-256 密钥，并使用 [KEY_ALIAS] 标记。
     * 该密钥仅限于 GCM 模式（不允许其他分组模式），
     * 且仅用于加密/解密目的——不能用于签名或密钥包装。
     */
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
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE) // GCM 模式不需要填充
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        keyGenerator.generateKey()
    }

    /**
     * 通过别名从密钥库中检索 AES 密钥。
     *
     * 密钥材料永远不会进入应用进程——密钥库返回一个不透明引用，
     * [Cipher] 通过 TEE/SE 硬件（在可用时）使用该引用。
     */
    private fun getSecretKey(): SecretKey {
        return (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
    }

    /**
     * 加密 [inputFile] 的内容，并将结果写入 [outputFile]。
     *
     * 输出文件将包含 12 字节 IV，后跟 GCM 密文
     * （末尾包含 128 位认证标签）。
     *
     * @param inputFile  要加密的明文文件。不会被修改。
     * @param outputFile 目标文件；创建或覆盖。
     */
    fun encryptFile(inputFile: File, outputFile: File) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())

        FileInputStream(inputFile).use { input ->
            FileOutputStream(outputFile).use { output ->
                // 将随机生成的 IV 作为前 12 字节写入
                output.write(cipher.iv)
                // 包装原始输出流，使后续所有写入都经过加密
                CipherOutputStream(output, cipher).use { cos ->
                    input.copyTo(cos, DEFAULT_BUFFER_SIZE)
                }
            }
        }
    }

    /**
     * 解密 [inputFile]（先前由 [encryptFile] 加密），并将明文写入 [outputFile]。
     *
     * 从文件开头读取 12 字节 IV，然后使用该 IV 和 GCM 标签长度
     * 以 DECRYPT_MODE 初始化 cipher。如果文件被篡改，
     * [CipherInputStream] 将抛出 AEADBadTagException。
     *
     * @param inputFile  加密文件（IV + 密文）。
     * @param outputFile 恢复后的明文目标位置。
     */
    fun decryptFile(inputFile: File, outputFile: File) {
        FileInputStream(inputFile).use { input ->
            // 读取加密时添加的 IV
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

    /**
     * 将任意 [InputStream] 加密到 [outputStream] 中。
     *
     * 适用于在写入磁盘之前即时加密网络响应，
     * 避免将整个明文缓冲在内存中。
     *
     * @param inputStream  明文字节源。此方法不会关闭该流。
     * @param outputStream IV + 密文的目标位置。此方法不会关闭该流。
     */
    fun encryptStream(inputStream: InputStream, outputStream: OutputStream) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        outputStream.write(cipher.iv)
        CipherOutputStream(outputStream, cipher).use { cos ->
            inputStream.copyTo(cos, DEFAULT_BUFFER_SIZE)
        }
    }

    /**
     * 将 [InputStream]（IV + 密文）解密到 [outputStream] 中。
     *
     * 从流的开头读取 12 字节 IV，然后解密剩余部分。
     * 如果密文被篡改（GCM 认证失败），则抛出异常。
     *
     * @param inputStream  加密字节源。此方法不会关闭该流。
     * @param outputStream 明文的目标位置。此方法不会关闭该流。
     */
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
        /** Android 密钥库提供者名称。必须与框架使用的字符串匹配。 */
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"

        /** AES 密钥在密钥库中存储的别名。 */
        private const val KEY_ALIAS = "sourcehub_file_encryption_key"

        /** JCE 转换字符串：AES 在 GCM 模式下，无填充。 */
        private const val TRANSFORMATION = "AES/GCM/NoPadding"

        /** GCM 推荐 IV 长度（96 位 / 12 字节）。 */
        private const val GCM_IV_LENGTH = 12

        /** GCM 认证标签长度（128 位）。 */
        private const val GCM_TAG_LENGTH = 128

        /** 用于流复制操作的 8 KiB 缓冲区。 */
        private const val DEFAULT_BUFFER_SIZE = 8192
    }
}
