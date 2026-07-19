package com.sourcehub.server

import com.sourcehub.server.config.AppConfig
import com.sourcehub.server.plugins.*
import com.sourcehub.server.routes.*
import com.sourcehub.server.security.JwtManager
import io.ktor.serialization.gson.*
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

fun main() {
    embeddedServer(Netty, port = AppConfig.fromEnvironment().port, host = "0.0.0.0") {
        module()
    }.start(wait = true)
}

fun Application.module() {
    val appConfig = AppConfig.fromEnvironment()
    val db = DatabaseFactory.init(appConfig)
    val jwtManager = JwtManager(appConfig)

    install(DefaultHeaders)
    install(Compression)
    install(CORS) {
        anyHost()
        allowCredentials = true
        allowNonSimpleContentTypes = true
    }
    install(ContentNegotiation) {
        gson { setPrettyPrinting() }
    }
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respond(mapOf("code" to 500, "message" to (cause.message ?: "Internal error")))
        }
    }
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

    routing {
        authRoutes(jwtManager, db)
        bannerCategoryRoutes(db)
        productRoutes(db)
        orderRoutes(jwtManager, db)
        paymentRoutes(jwtManager, db)
        fileRoutes(jwtManager, db, appConfig)
        get("/api/health") { call.respond(mapOf("status" to "ok")) }
    }
}
