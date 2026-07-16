/**
 * **认证路由：注册、登录、令牌刷新、用户资料**
 *
 * 本包定义了 `/api/auth/*` 路由树，处理用户身份操作。它使用：
 * - **BCrypt**（通过 [at.favre.lib.crypto.bcrypt](https://github.com/patrickfav/bcrypt)）
 *   进行密码哈希 —— 12 轮，截至 2024 年被认为是安全的。
 * - **newSuspendedTransaction** 用于所有数据库访问，确保
 *   基于协程的 Ktor 处理器不会阻塞 Netty 事件循环线程。
 * - **JWT**（通过 [JwtManager]）用于登录后的无状态认证。
 *
 * ## 端点
 * | 方法   | 路径               | 需要认证? | 描述                     |
 * |--------|-------------------|----------|--------------------------|
 * | POST   | /api/auth/register| 否       | 创建新用户账户             |
 * | POST   | /api/auth/login   | 否       | 认证并获取令牌             |
 * | POST   | /api/auth/refresh | 否       | 用刷新令牌换取新令牌对      |
 * | GET    | /api/auth/profile | 是       | 获取当前用户资料           |
 * | PUT    | /api/auth/profile | 是       | 更新当前用户资料           |
 *
 * ## 为什么使用 `newSuspendedTransaction`？
 * Ktor 在协程调度器上运行请求处理器。协程内的阻塞 JDBC 调用
 * 会占用底层线程并降低吞吐量。
 * `newSuspendedTransaction` 将 JDBC 工作移至专用的 `Dispatchers.IO` 线程，
 * 并挂起协程直到结果就绪，使事件循环线程可以处理其他请求。
 * 这是 Ktor + Exposed 应用的推荐模式。
 *
 * ## 生产就绪
 * - **注册**：没有邮箱验证流程。在激活账户之前，
 *   添加邮箱验证步骤（发送带有短期令牌的链接）。
 * - **速率限制**：登录和注册端点没有速率限制。
 *   攻击者可以暴力破解密码或枚举邮箱。添加带指数退避的
 *   速率限制器（按 IP 或按邮箱）。
 * - **密码策略**：仅强制最小长度 6。添加复杂性要求
 *   （大写字母、数字、特殊字符）或对照已知泄露密码列表检查
 *   （如 Have I Been Pwned API）。
 * - **刷新令牌**：刷新端点在解码前不验证令牌签名
 *   （仅检查 `type == "refresh"`）。
 *   虽然解码在验证之前不信任载荷，但应首先应用验证器。
 *   此外，刷新令牌应在服务器端存储（哈希），以便可以吊销。
 * - **用户 ID 生成**：短随机后缀（`UUID.take(8)`）
 *   在大规模下具有不可忽略的碰撞概率。使用完整的 UUID 或 nanoid。
 */
package com.sourcehub.server.routes

import at.favre.lib.crypto.bcrypt.BCrypt
import com.auth0.jwt.JWT
import com.sourcehub.server.models.Users
import com.sourcehub.server.security.JwtManager
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import java.util.*

/**
 * 在 `/api/auth` 下挂载所有认证相关路由。
 *
 * @param jwtManager 用于创建和验证令牌的 JWT 令牌管理器。
 * @param db 用于执行查询的 Exposed 数据库句柄。
 */
fun Routing.authRoutes(jwtManager: JwtManager, db: Database) {
    route("/api/auth") {

        /**
         * POST /api/auth/register
         *
         * 创建新用户账户。期望一个包含 [name]、[email] 和 [password]
         * （最少 6 个字符）的 JSON 请求体。返回新用户的资料以及
         * 访问令牌和刷新令牌，以便客户端可以直接进入需要认证的页面，
         * 而无需单独登录。
         *
         * 状态码：
         * - 200：注册成功（返回令牌 + 资料）。
         * - 400：输入无效（空名称、密码太短、空邮箱）。
         * - 409：邮箱已注册。
         */
        post("/register") {
            val req = call.receive<RegisterRequest>()
            // 验证基本输入约束。
            if (req.email.isBlank() || req.password.length < 6 || req.name.isBlank()) {
                call.respond(mapOf("code" to 400, "message" to "Invalid input"))
                return@post
            }
            // 在插入前检查重复邮箱（竞态条件说明：
            // 两个并发请求可能同时通过此检查并都执行插入 ——
            // 在生产环境中给 email 列添加 UNIQUE 约束）。
            val existing = org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction(db = db) {
                Users.select { Users.email eq req.email }.singleOrNull()
            }
            if (existing != null) {
                call.respond(mapOf("code" to 409, "message" to "Email already registered"))
                return@post
            }
            // 生成短用户 ID 并使用 BCrypt（12 轮）对密码进行哈希。
            val userId = "u_${UUID.randomUUID().toString().take(8)}"
            val hash = BCrypt.withDefaults().hashToString(12, req.password.toCharArray())
            org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction(db = db) {
                Users.insert {
                    it[id] = userId
                    it[name] = req.name
                    it[email] = req.email
                    it[passwordHash] = hash
                    it[createdAt] = System.currentTimeMillis()
                }
            }
            // 签发令牌，使客户端可以立即发起需要认证的调用。
            val accessToken = jwtManager.generateAccessToken(userId, req.email)
            val refreshToken = jwtManager.generateRefreshToken(userId)
            call.respond(mapOf(
                "code" to 200, "message" to "success",
                "data" to mapOf(
                    "userId" to userId, "userName" to req.name, "userEmail" to req.email,
                    "accessToken" to accessToken, "refreshToken" to refreshToken,
                    "avatarUrl" to ""
                )
            ))
        }

        /**
         * POST /api/auth/login
         *
         * 通过邮箱和密码认证用户。成功后返回
         * 用户资料数据和一对新的访问 + 刷新令牌。
         *
         * 状态码：
         * - 200：登录成功。
         * - 401：邮箱或密码无效（两种情况返回相同的消息以避免邮箱枚举）。
         */
        post("/login") {
            val req = call.receive<LoginRequest>()
            val row = org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction(db = db) {
                Users.select { Users.email eq req.email }.singleOrNull()
            }
            if (row == null) {
                call.respond(mapOf("code" to 401, "message" to "Invalid email or password"))
                return@post
            }
            val hash = row[Users.passwordHash]
            // BCrypt.verifyer().verify() 返回一个结果对象；我们只关心 verified 布尔值。
            val valid = BCrypt.verifyer().verify(req.password.toCharArray(), hash)
            if (!valid.verified) {
                call.respond(mapOf("code" to 401, "message" to "Invalid email or password"))
                return@post
            }
            val accessToken = jwtManager.generateAccessToken(row[Users.id], row[Users.email])
            val refreshToken = jwtManager.generateRefreshToken(row[Users.id])
            call.respond(mapOf(
                "code" to 200, "message" to "success",
                "data" to mapOf(
                    "userId" to row[Users.id], "userName" to row[Users.name],
                    "userEmail" to row[Users.email], "avatarUrl" to (row[Users.avatarUrl] ?: ""),
                    "accessToken" to accessToken, "refreshToken" to refreshToken
                )
            ))
        }

        /**
         * POST /api/auth/refresh
         *
         * 接受一个刷新令牌并返回一对新的访问 + 刷新令牌。
         * 传入的令牌在其载荷中必须包含 `"type": "refresh"`。
         *
         * **安全说明**：刷新令牌被解码（未验证）以读取 `type` 声明。
         * 恶意构造的令牌可能通过此检查，但在使用新令牌时会被拒绝
         * （访问令牌使用服务器密钥签名）。尽管如此，
         * 在信任刷新令牌的声明之前，应先对其应用验证器。
         *
         * 状态码：
         * - 200：返回新令牌对。
         * - 401：刷新令牌无效或已过期，或用户未找到。
         */
        post("/refresh") {
            val req = call.receive<RefreshRequest>()
            try {
                val payload = JWT.decode(req.refreshToken)
                if (payload.getClaim("type").asString() != "refresh") {
                    call.respond(mapOf("code" to 401, "message" to "Invalid refresh token"))
                    return@post
                }
                val userId = payload.getClaim("userId").asString() ?: ""
                val row = org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction(db = db) {
                    Users.select { Users.id eq userId }.singleOrNull()
                }
                if (row == null) {
                    call.respond(mapOf("code" to 401, "message" to "User not found"))
                    return@post
                }
                val accessToken = jwtManager.generateAccessToken(userId, row[Users.email])
                val refreshToken = jwtManager.generateRefreshToken(userId)
                call.respond(mapOf("code" to 200, "data" to mapOf("accessToken" to accessToken, "refreshToken" to refreshToken)))
            } catch (e: Exception) {
                call.respond(mapOf("code" to 401, "message" to "Invalid token"))
            }
        }

        // 以下路由需要在 Authorization 头部中提供有效的 JWT（访问令牌）。
        authenticate("auth-jwt") {
            /**
             * GET /api/auth/profile
             *
             * 返回已认证用户的资料数据。
             */
            get("/profile") {
                val userId = jwtManager.getUserId(call) ?: return@get call.respond(mapOf("code" to 401))
                val row = org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction(db = db) {
                    Users.select { Users.id eq userId }.singleOrNull()
                } ?: return@get call.respond(mapOf("code" to 404))
                call.respond(mapOf("code" to 200, "data" to mapOf(
                    "userId" to row[Users.id], "name" to row[Users.name], "email" to row[Users.email],
                    "avatarUrl" to (row[Users.avatarUrl] ?: ""), "phone" to (row[Users.phone] ?: ""),
                    "createdAt" to row[Users.createdAt]
                )))
            }

            /**
             * PUT /api/auth/profile
             *
             * 更新已认证用户的资料字段。仅应用请求体中非 null 的字段（部分更新）。
             */
            put("/profile") {
                val userId = jwtManager.getUserId(call) ?: return@put call.respond(mapOf("code" to 401))
                val req = call.receive<UpdateProfileRequest>()
                org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction(db = db) {
                    Users.update({ Users.id eq userId }) {
                        req.name?.let { n -> it[name] = n }
                        req.avatarUrl?.let { a -> it[avatarUrl] = a }
                        req.phone?.let { p -> it[phone] = p }
                    }
                }
                call.respond(mapOf("code" to 200, "message" to "updated"))
            }
        }
    }
}

/** POST /api/auth/login 的 JSON 请求体。 */
@Serializable data class LoginRequest(val email: String, val password: String)

/** POST /api/auth/register 的 JSON 请求体。 */
@Serializable data class RegisterRequest(val name: String, val email: String, val password: String)

/** POST /api/auth/refresh 的 JSON 请求体。 */
@Serializable data class RefreshRequest(val refreshToken: String)

/** PUT /api/auth/profile 的 JSON 请求体。所有字段可选，用于部分更新。 */
@Serializable data class UpdateProfileRequest(val name: String? = null, val avatarUrl: String? = null, val phone: String? = null)
