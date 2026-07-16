/**
 * **通过环境变量的应用程序配置**
 *
 * 本包为 SourceHub 服务器中的每个可调参数提供单一数据源。
 * 所有值都从环境变量中读取，并带有合理的开发默认值，
 * 使得在 Docker、CI 或生产环境中无需修改代码即可覆盖设置。
 *
 * ## 设计理念
 * - **不使用配置文件库**（HOCON、YAML 等）。使用 `System.getenv()` 保持简洁，
 *   避免了额外的依赖，并使配置项在单个文件中一目了然。
 * - **不可变数据类** -- [AppConfig] 是一个普通的 `data class`，
 *   因此可以安全传递，且易于在测试中构造。
 * - **伴生对象工厂** -- [AppConfig.fromEnvironment] 是规范的构造函数。
 *   数据类本身的命名参数默认值作为每个字段含义的文档。
 *
 * ## 生产环境注意事项
 * - **jwtSecret** 默认为硬编码字符串用于本地开发。
 *   在每个部署环境中必须用强随机密钥（如 256 位 base64）覆盖。
 * - **dbPassword** 默认为空（H2 文件模式不需要密码）。
 *   切换到 PostgreSQL/MySQL 时，请通过密钥管理器设置 `DB_PASSWORD`。
 */
package com.sourcehub.server.config

/**
 * SourceHub 服务器的中央配置持有者。
 *
 * 每个字段都从环境变量中读取，变量名记录在
 * [伴生对象工厂][AppConfig.fromEnvironment] 中。数据类本身设计为可在测试中
 * 直接用自定义值实例化。
 *
 * @property port HTTP 监听端口（默认：8080）
 * @property jwtSecret JWT 令牌的 HMAC-SHA256 签名密钥
 * @property jwtIssuer 已签发令牌中的 "iss" 声明值
 * @property jwtAudience 已签发令牌中的 "aud" 声明值
 * @property accessTokenLifetimeMs 访问令牌有效期，单位毫秒（默认：15 分钟）
 * @property refreshTokenLifetimeMs 刷新令牌有效期，单位毫秒（默认：7 天）
 * @property dbDriver JDBC 驱动类名（默认：H2）
 * @property dbUrl JDBC 连接 URL（默认：H2 文件数据库）
 * @property dbUser 数据库用户名（默认：H2 为 "sa"）
 * @property dbPassword 数据库密码（默认：H2 为空）
 * @property uploadDir 上传/可下载文件的本地目录
 * @property maxFileSizeMB 最大允许文件上传大小，单位 MB
 */
data class AppConfig(
    val port: Int,
    val jwtSecret: String,
    val jwtIssuer: String,
    val jwtAudience: String,
    val accessTokenLifetimeMs: Long,
    val refreshTokenLifetimeMs: Long,
    val dbDriver: String,
    val dbUrl: String,
    val dbUser: String,
    val dbPassword: String,
    val uploadDir: String,
    val maxFileSizeMB: Long
) {
    companion object {
        /**
         * 通过从相应的环境变量读取每个值来构建 [AppConfig]。
         * 如果变量未设置，则使用已记录的开发时默认值。
         *
         * 映射关系：
         * - `PORT` -> [port]
         * - `JWT_SECRET` -> [jwtSecret]
         * - `JWT_ISSUER` -> [jwtIssuer]
         * - `JWT_AUDIENCE` -> [jwtAudience]
         * - `ACCESS_TOKEN_LIFETIME_MS` -> [accessTokenLifetimeMs]
         * - `REFRESH_TOKEN_LIFETIME_MS` -> [refreshTokenLifetimeMs]
         * - `DB_DRIVER` -> [dbDriver]
         * - `DB_URL` -> [dbUrl]
         * - `DB_USER` -> [dbUser]
         * - `DB_PASSWORD` -> [dbPassword]
         * - `UPLOAD_DIR` -> [uploadDir]
         * - `MAX_FILE_SIZE_MB` -> [maxFileSizeMB]
         */
        fun fromEnvironment(): AppConfig {
            return AppConfig(
                port = env("PORT", "8080").toInt(),
                // 警告：默认 JWT 密钥不安全；生产环境中请覆盖！
                jwtSecret = env("JWT_SECRET", "sourcehub-jwt-secret-change-in-production"),
                jwtIssuer = env("JWT_ISSUER", "sourcehub-server"),
                jwtAudience = env("JWT_AUDIENCE", "sourcehub-app"),
                // 访问令牌 15 分钟有效 —— 如果泄露，时间窗口较短
                accessTokenLifetimeMs = env("ACCESS_TOKEN_LIFETIME_MS", "900000").toLong(),
                // 刷新令牌 7 天有效 —— 方便移动端用户
                refreshTokenLifetimeMs = env("REFRESH_TOKEN_LIFETIME_MS", "604800000").toLong(),
                // H2 用于本地开发；生产环境中替换为 PostgreSQL/MySQL
                dbDriver = env("DB_DRIVER", "org.h2.Driver"),
                // AUTO_SERVER=TRUE 允许多个进程并发连接
                dbUrl = env("DB_URL", "jdbc:h2:file:./data/sourcehub;AUTO_SERVER=TRUE"),
                dbUser = env("DB_USER", "sa"),
                dbPassword = env("DB_PASSWORD", ""),
                uploadDir = env("UPLOAD_DIR", "./uploads"),
                maxFileSizeMB = env("MAX_FILE_SIZE_MB", "50").toLong()
            )
        }

        /**
         * 从进程环境中读取 [key]，当变量不存在时回退到 [default]。
         */
        private fun env(key: String, default: String): String =
            System.getenv(key) ?: default
    }
}
