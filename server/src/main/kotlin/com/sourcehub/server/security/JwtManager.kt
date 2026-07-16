/**
 * **SourceHub 认证的 JWT 令牌管理**
 *
 * 本包封装了服务器使用的所有 JSON Web Token (JWT) 操作。
 * 它使用 [Auth0 java-jwt](https://github.com/auth0/java-jwt) 库
 * 来创建和验证 HS256 签名的令牌。
 *
 * ## 令牌策略
 * 服务器签发**两种**类型的令牌：
 * 1. **访问令牌** -- 短期有效（默认 15 分钟），携带 `userId` 和 `email` 声明。
 *    在每个需要认证的请求中通过 `Authorization: Bearer <token>` 头部发送。
 * 2. **刷新令牌** -- 长期有效（默认 7 天），携带 `userId` 和 `type: "refresh"` 声明。
 *    仅在 `/api/auth/refresh` 端点使用，用于获取新的访问令牌而无需重新输入凭据。
 *
 * 这种分离遵循 OAuth 2.0 / OpenID Connect 最佳实践：
 * 频繁传输的访问令牌具有较短的生命周期以限制泄露造成的损害，
 * 而刷新令牌可以更安全地存储且使用频率较低。
 *
 * ## 关键设计决策
 * - 使用 **HS256 (HMAC-SHA256)** 而非 RS256，因为服务器既签发又验证令牌
 *   （对称密钥是可接受的）。如果第三方需要验证令牌（如微服务），请切换到 RS256 或 ES256。
 * - **未实现令牌吊销列表**。令牌在过期之前一直有效。在生产环境中，
 *   考虑使用 Redis 支持的吊销列表来处理受损令牌，或实现刷新令牌轮换。
 * - **验证器**在构造函数中预构建并在所有请求中重用 ——
 *   构建它的成本相对较高（加密设置），因此我们只构建一次。
 *
 * ## 生产就绪
 * - 如果令牌验证需要在多个服务或客户端中进行，将 HS256 替换为 **RS256** 或 **ES256**。
 * - 添加**令牌吊销列表**（Redis 存储已吊销 JTI 值的集合）以支持
 *   登出和账户受损场景。
 * - 实现**刷新令牌轮换**：每次使用时签发新的刷新令牌并使旧令牌失效。
 * - 将签名密钥存储在硬件安全模块（HSM）或云 KMS 中，而非环境变量中。
 */
package com.sourcehub.server.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.sourcehub.server.config.AppConfig
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import java.util.*

/**
 * 管理 SourceHub 服务器的 JWT 令牌创建和验证。
 *
 * @param config 提供密钥、签发者、受众和令牌生命周期的应用程序配置。
 */
class JwtManager(config: AppConfig) {
    private val secret = config.jwtSecret
    private val issuer = config.jwtIssuer
    private val audience = config.jwtAudience
    private val accessLifetime = config.accessTokenLifetimeMs
    private val refreshLifetime = config.refreshTokenLifetimeMs

    /**
     * 预构建的 JWT 验证器，供 Ktor 的 [Authentication] 插件使用。
     * 验证签名 (HS256)、签发者和受众声明。
     * 过期时间由 JWT 库自动检查。
     */
    val verifier = com.auth0.jwt.JWT.require(Algorithm.HMAC256(secret))
        .withIssuer(issuer)
        .withAudience(audience)
        .build()

    /**
     * 为指定用户创建短期访问令牌。
     *
     * @param userId 用户的唯一标识（来自 [com.sourcehub.server.models.Users.id]）。
     * @param email 用户的邮箱地址（用于审计/显示目的）。
     * @return 一个已签名的 JWT 字符串，有效期为 [accessLifetime] 毫秒。
     */
    fun generateAccessToken(userId: String, email: String): String {
        return JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withClaim("userId", userId)
            .withClaim("email", email)
            .withIssuedAt(Date())
            .withExpiresAt(Date(System.currentTimeMillis() + accessLifetime))
            .sign(Algorithm.HMAC256(secret))
    }

    /**
     * 为指定用户创建长期刷新令牌。
     *
     * 包含 `"type": "refresh"` 声明，以便刷新端点
     * 能够区分刷新令牌和访问令牌。
     *
     * @param userId 用户的唯一标识。
     * @return 一个已签名的 JWT 字符串，有效期为 [refreshLifetime] 毫秒。
     */
    fun generateRefreshToken(userId: String): String {
        return JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withClaim("userId", userId)
            // 区分刷新令牌和访问令牌，以便 /refresh
            // 端点可以拒绝访问令牌。
            .withClaim("type", "refresh")
            .withIssuedAt(Date())
            .withExpiresAt(Date(System.currentTimeMillis() + refreshLifetime))
            .sign(Algorithm.HMAC256(secret))
    }

    /**
     * 从 Ktor 认证插件附加到此 HTTP 调用的 JWT principal 中
     * 提取 `userId` 声明。
     *
     * 如果调用未经过认证（不存在有效的 JWT），则返回 `null`。
     *
     * @param call 当前的 Ktor 应用程序调用。
     * @return 令牌中的用户 ID 字符串，或 `null`。
     */
    fun getUserId(call: io.ktor.server.application.ApplicationCall): String? {
        val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
        return principal?.payload?.getClaim("userId")?.asString()
    }
}
