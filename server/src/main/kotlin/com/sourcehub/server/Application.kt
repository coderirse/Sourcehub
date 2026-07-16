/**
 * **SourceHub 服务器 - 应用程序入口**
 *
 * 本文件是支撑 SourceHub 数字产品市场的 Ktor HTTP 服务器的主入口。
 * 它启动一个 [Netty] 嵌入式服务器，通过安装每个 Ktor 插件并注册每个路由扩展来组装
 * 应用程序模块，并将三个核心依赖连接在一起：
 * - [AppConfig] -- 基于环境变量的配置
 * - [DatabaseFactory.init] -- Exposed/H2 数据库连接 + 表结构创建
 * - [JwtManager] -- JWT (HS256) 令牌的创建和验证
 *
 * ## 架构概览
 * module 函数遵循 Ktor 扩展函数组合的惯例：
 * 插件或路由扩展所需的一切都通过参数传递（没有全局 DI 框架）。
 * 每个 `install` 块配置一个横切关注点（CORS、内容协商、认证等），
 * 每个 `xxxRoutes(...)` 调用在 `/api/…` 下挂载一个 REST 资源树。
 *
 * ## 关键设计决策
 * - **Netty** 被选为引擎，因为它性能经过验证且支持无缝嵌入式模式（单 JAR 部署）。
 * - 使用 **JWT 认证** 而非会话，使服务器保持无状态 —— 这在 Android 客户端可能
 *   通过不同 IP / 网络条件重连时非常重要。
 * - **JSON 内容协商** 使用 `kotlinx.serialization`，因为 Ktor 原生支持它，
 *   且避免了依赖反射的替代方案。
 *
 * ## 生产就绪
 * - [install(StatusPages)] 中的 `exception<Throwable>` 处理器会向客户端泄露堆栈跟踪。
 *   在生产环境中应将其缩小为仅处理自定义应用程序异常，并对意外错误使用通用回退。
 * - CORS 配置了 `anyHost()`，这在开发时很方便，但在生产环境中应限制为实际的前端源。
 * - 敏感信息（JWT 密钥、数据库密码）来自 [AppConfig.fromEnvironment]，
 *   必须从安全的密钥存储（Vault、K8s secrets 等）中获取，而不是纯环境变量。
 */
package com.sourcehub.server

import com.sourcehub.server.config.AppConfig
import com.sourcehub.server.plugins.*
import com.sourcehub.server.routes.*
import com.sourcehub.server.security.JwtManager
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json

/**
 * 应用程序入口 —— 在通过 [AppConfig] 配置的端口和主机上启动嵌入式 Netty HTTP 服务器。
 *
 * 服务器绑定到 `0.0.0.0`，以便局域网内的其他设备可以访问
 * （便于在桌面开发机上测试 Android 客户端）。
 */
fun main() {
    embeddedServer(Netty, port = AppConfig.fromEnvironment().port, host = "0.0.0.0") {
        module()
    }.start(wait = true)
}

/**
 * 组装完整的 Ktor 应用程序模块。
 *
 * 每个插件和路由扩展都通过构造函数/函数参数显式接收其依赖项，
 * 而不是通过 DI 容器。这使依赖关系可见且易于理解。
 *
 * 插件按以下顺序安装：
 * 1. [DefaultHeaders] -- 添加标准安全头部
 * 2. [Compression] -- 对响应进行透明的 gzip/deflate 压缩
 * 3. [CORS] -- 跨域规则（当前为开发环境开放）
 * 4. [ContentNegotiation] -- 通过 kotlinx.serialization 进行 JSON 序列化
 * 5. [StatusPages] -- 全局异常到 JSON 的映射
 * 6. [Authentication] -- JWT 不记名令牌验证
 *
 * 路由树随后挂载在 `/api/…` 下。
 */
fun Application.module() {
    val appConfig = AppConfig.fromEnvironment()
    val db = DatabaseFactory.init(appConfig)
    val jwtManager = JwtManager(appConfig)

    // 标准安全头部（X-Content-Type-Options 等）
    install(DefaultHeaders)

    // 对超过阈值的响应进行 Gzip/deflate 压缩
    install(Compression)

    // 警告：anyHost() 仅适用于开发环境。生产环境中请限制。
    install(CORS) {
        anyHost()
        allowCredentials = true
        allowNonSimpleContentTypes = true
    }

    // JSON 序列化使用 kotlinx.serialization —— 无需反射。
    // 开发期间启用 prettyPrint 以便于调试。
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true; prettyPrint = true; isLenient = true })
    }

    // 全局异常处理器：捕获任何未处理的 Throwable 并返回 JSON 错误信封。
    // 在生产环境中，应区分应用程序级异常（400/401/403/404）和意外的 500 错误。
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respond(mapOf("code" to 500, "message" to (cause.message ?: "Internal error")))
        }
    }

    // JWT 认证提供者。令牌通过 JwtManager 构建的 HS256 验证器进行验证。
    // validate 块确保令牌包含非空的 "userId" 声明；否则认证被拒绝。
    install(Authentication) {
        jwt("auth-jwt") {
            verifier(jwtManager.verifier)
            realm = "SourceHub API"
            validate { credential ->
                if (credential.payload.getClaim("userId").asString() != null)
                    JWTPrincipal(credential.payload)
                else null
            }
        }
    }

    // 挂载所有 REST 资源树 + 一个简单的健康检查端点。
    routing {
        authRoutes(jwtManager, db)
        productRoutes(db)
        orderRoutes(jwtManager, db)
        paymentRoutes(jwtManager, db)
        fileRoutes(jwtManager, db, appConfig)
        get("/api/health") { call.respond(mapOf("status" to "ok")) }
    }
}
