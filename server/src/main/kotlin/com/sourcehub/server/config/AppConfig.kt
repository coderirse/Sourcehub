package com.sourcehub.server.config

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
        fun fromEnvironment(): AppConfig {
            return AppConfig(
                port = env("PORT", "8080").toInt(),
                jwtSecret = env("JWT_SECRET", "sourcehub-jwt-secret-change-in-production"),
                jwtIssuer = env("JWT_ISSUER", "sourcehub-server"),
                jwtAudience = env("JWT_AUDIENCE", "sourcehub-app"),
                accessTokenLifetimeMs = env("ACCESS_TOKEN_LIFETIME_MS", "900000").toLong(),
                refreshTokenLifetimeMs = env("REFRESH_TOKEN_LIFETIME_MS", "604800000").toLong(),
                dbDriver = env("DB_DRIVER", "org.h2.Driver"),
                dbUrl = env("DB_URL", "jdbc:h2:file:./data/sourcehub;AUTO_SERVER=TRUE"),
                dbUser = env("DB_USER", "sa"),
                dbPassword = env("DB_PASSWORD", ""),
                uploadDir = env("UPLOAD_DIR", "./uploads"),
                maxFileSizeMB = env("MAX_FILE_SIZE_MB", "50").toLong()
            )
        }

        private fun env(key: String, default: String): String =
            System.getenv(key) ?: default
    }
}
