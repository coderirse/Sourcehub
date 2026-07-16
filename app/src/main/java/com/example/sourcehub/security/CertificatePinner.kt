/**
 * SourceHub OkHttp 客户端的 SSL/TLS 证书锁定。
 *
 * 证书锁定将 OkHttp 客户端绑定到一组特定的 X.509 证书公钥，
 * 使应用拒绝连接到提供不同证书的服务器——即使该证书
 * 被平台 CA 存储信任。这可以防御依赖用户安装或受损 CA 证书的
 * 中间人攻击。
 *
 * ## MVP 状态
 * 此文件中的锁定值是**占位符**。在发布生产版本之前，
 * 必须将 `sha256/AAAA...` 固定值替换为服务器证书的
 * Subject Public Key Info (SPKI) 的实际 SHA-256 哈希值，可通过以下方式获取：
 * ```
 * openssl s_client -servername api.sourcehub.example.com -connect api.sourcehub.example.com:443 | \
 *   openssl x509 -pubkey -noout | \
 *   openssl pkey -pubin -outform der | \
 *   openssl dgst -sha256 -binary | \
 *   openssl enc -base64
 * ```
 *
 * ## 最佳实践
 * - 始终固定至少两个密钥：活跃证书和一个离线保存的备用密钥，
 *   以便证书轮换不会破坏现有应用安装。
 * - 仅在连接后端的客户端实例上设置 [okhttp3.OkHttpClient.Builder.certificatePinner]，
 *   不要在用于第三方 CDN 或分析服务的客户端上设置。
 */
package com.example.sourcehub.security

import okhttp3.CertificatePinner as OkHttpCertificatePinner
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * 构建一个带有证书锁定和合理超时时间的 [OkHttpClient]。
 *
 * 返回的客户端将请求锁定到 `api.sourcehub.example.com`，使得
 * 持有不同证书的恶意代理无法拦截流量。
 */
object CertificatePinner {

    /**
     * 创建一个带有 30 秒连接/读/写超时的固定证书 [OkHttpClient]。
     *
     * 证书锁定当前为**占位符**，会拒绝所有连接。
     * 在从 Mock 切换到远程 API 模式之前，
     * 将固定字符串替换为真实后端的 SPKI 哈希值。
     *
     * @return 启用了证书锁定的新 [OkHttpClient] 实例。
     */
    fun buildPinnedClient(): OkHttpClient {
        val certificatePinner = OkHttpCertificatePinner.Builder()
            // MVP：占位符固定值——在生产环境前替换为真实服务器固定值
            .add("api.sourcehub.example.com", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
            .build()

        return OkHttpClient.Builder()
            .certificatePinner(certificatePinner)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
