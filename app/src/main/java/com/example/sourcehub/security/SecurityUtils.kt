/**
 * SourceHub 应用的通用密码学工具。
 *
 * 提供：
 * - **HMAC-SHA256 请求签名**，用于 API 请求完整性验证。
 * - **通过 [SecureRandom] 生成密码学随机数（nonce）**，用于防重放攻击。
 * - **SHA-256 哈希**（Base64 编码），用于数据完整性校验。
 * - **UUID 生成**，用于客户端 ID 分配。
 *
 * ## HMAC 密钥管理
 * HMAC 密钥（[HMAC_SECRET]）目前是编译时常量——这对于
 * MVP/Mock 后端阶段是可接受的。在部署到生产环境之前，密钥必须
 * 迁移到原生代码中（通过 Android NDK）或在运行时从密钥库中获取，
 * 使其无法通过 `strings` 或反编译器从 APK 中简单地提取。
 */
package com.example.sourcehub.security

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * 无状态的密码学辅助函数集合。
 *
 * 每个方法都是纯函数（给定相同输入，产生相同输出，
 * 除了 [generateNonce] 和 [generateUuid] 有意使用随机值）。
 * 不维护任何实例状态。
 */
object SecurityUtils {

    /**
     * 用于请求签名的共享 HMAC 密钥。
     *
     * **安全注意**：这是为 MVP 方便的编译时常量。
     * 在生产环境中，应从原生代码或 Android 密钥库中加载，
     * 使其无法从逆向工程的 APK 中恢复。
     */
    private const val HMAC_SECRET = "sourcehub_mock_secret_key_2024" // 生产环境中：存储在原生代码中

    /** HMAC-SHA256 的 JCE 算法名称。 */
    private const val ALGORITHM = "HmacSHA256"

    /**
     * 对拼接的请求参数计算 HMAC-SHA256 签名。
     *
     * 签名负载为：`"METHOD|path|timestamp|body"`，以竖线分隔。
     * 服务器必须重建相同的字符串并计算 HMAC 以验证
     * 请求在传输过程中未被篡改。
     *
     * @param method    HTTP 方法，大写（例如 `"POST"`）。
     * @param path      请求路径（例如 `"/api/v1/products"`）。
     * @param timestamp 请求时的 Unix 纪元毫秒数（防重放保护）。
     * @param body      原始请求体字符串（GET 请求为空字符串）。
     * @return Base64 编码的 HMAC-SHA256 签名（无换行）。
     */
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

    /**
     * 生成密码学安全的随机数（nonce），适用于认证挑战或
     * 请求去重头中的一次性令牌。
     *
     * @param length 要生成的随机字节数（默认 16，即 128 位）。
     * @return Base64 编码的 nonce 字符串（无换行）。
     */
    fun generateNonce(length: Int = 16): String {
        val random = SecureRandom()
        val bytes = ByteArray(length)
        random.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    /**
     * 计算 [input] 的 SHA-256 哈希，并将结果作为 Base64 字符串返回。
     *
     * 这是单向哈希——请勿用于密码（应在服务器端使用 bcrypt/argon2）；
     * 适用于完整性验证（例如校验下载的文件）。
     *
     * @param input 要哈希的任意字符串。
     * @return Base64 编码的 SHA-256 摘要。
     */
    fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }

    /**
     * 生成随机类型 4 UUID 字符串。
     *
     * 用于在服务器分配其规范 ID 之前分配客户端 ID
     * （例如购物车项、草稿订单）。
     *
     * @return 格式为 `xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx` 的 UUID 字符串。
     */
    fun generateUuid(): String {
        return java.util.UUID.randomUUID().toString()
    }
}
