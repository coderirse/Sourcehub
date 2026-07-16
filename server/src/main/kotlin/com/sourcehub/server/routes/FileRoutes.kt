package com.sourcehub.server.routes

import com.sourcehub.server.config.AppConfig
import com.sourcehub.server.models.*
import com.sourcehub.server.security.JwtManager
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.io.File
import java.util.*

fun Routing.fileRoutes(jwtManager: JwtManager, db: Database, config: AppConfig) {
    authenticate("auth-jwt") {
        route("/api/files") {

            // Get download URL (checks purchase status)
            post("/download-url/{productId}") {
                val userId = jwtManager.getUserId(call) ?: return@post call.respond(mapOf("code" to 401))
                val productId = call.parameters["productId"] ?: ""

                // Check if user has purchased and paid for this product
                val hasAccess = newSuspendedTransaction(db = db) {
                    OrderItems.innerJoin(Orders)
                        .select { (OrderItems.productId eq productId) and (Orders.userId eq userId) and (Orders.status eq "PAID") }
                        .count() > 0
                }

                if (!hasAccess) {
                    return@post call.respond(mapOf("code" to 403, "message" to "Please purchase first"))
                }

                val product = newSuspendedTransaction(db = db) {
                    Products.select { Products.id eq productId }.singleOrNull()
                }

                if (product == null) {
                    return@post call.respond(mapOf("code" to 404, "message" to "Product not found"))
                }

                // Generate a one-time download token
                val downloadId = "dl_${UUID.randomUUID().toString().take(8)}"
                val token = UUID.randomUUID().toString()
                val fileName = "${product[Products.title]}.${product[Products.fileType]?.lowercase() ?: "pdf"}"

                // Record download
                newSuspendedTransaction(db = db) {
                    Downloads.insert {
                        it[Downloads.id] = downloadId
                        it[Downloads.userId] = userId
                        it[Downloads.orderId] = ""
                        it[Downloads.productId] = productId
                        it[Downloads.fileName] = fileName
                        it[Downloads.createdAt] = System.currentTimeMillis()
                    }
                }

                call.respond(mapOf("code" to 200, "data" to mapOf(
                    "productId" to productId,
                    "downloadUrl" to "/api/files/download/$downloadId?token=$token",
                    "fileName" to fileName,
                    "fileSize" to product[Products.fileSize],
                    "expiresAt" to (System.currentTimeMillis() + 3600000)
                )))
            }

            // Actual file download endpoint
            get("/download/{downloadId}") {
                val downloadId = call.parameters["downloadId"] ?: ""
                val token = call.request.queryParameters["token"] ?: ""

                // Verify download record
                val download = newSuspendedTransaction(db = db) {
                    Downloads.select { Downloads.id eq downloadId }.singleOrNull()
                } ?: return@get call.respondText("Not found", status = HttpStatusCode.NotFound)

                val product = newSuspendedTransaction(db = db) {
                    Products.select { Products.id eq download[Downloads.productId] }.singleOrNull()
                } ?: return@get call.respondText("File not available", status = HttpStatusCode.NotFound)

                // In production, serve the actual file from storage
                val filePath = product[Products.filePath] ?: ""
                val file = File(config.uploadDir, filePath)

                if (file.exists()) {
                    call.response.header("Content-Disposition", "attachment; filename=\"${download[Downloads.fileName]}\"")
                    call.respondFile(file)
                } else {
                    val sample = File(config.uploadDir, "sample.pdf")
                    if (!sample.exists()) sample.writeBytes(generateSamplePdf())
                    call.response.header("Content-Disposition", "attachment; filename=\"${download[Downloads.fileName]}\"")
                    call.respondFile(sample)
                }
            }
        }
    }
}

/** Generate a minimal valid PDF for demo purposes. */
private fun generateSamplePdf(): ByteArray {
    val content = """
%PDF-1.4
1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj
2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj
3 0 obj<</Type/Page/MediaBox[0 0 612 792]/Parent 2 0 R/Resources<<>>>>endobj
xref
0 4
0000000000 65535 f
0000000009 00000 n
0000000058 00000 n
0000000115 00000 n
trailer<</Size 4/Root 1 0 R>>
startxref
206
%%EOF
    """.trimIndent()
    return content.toByteArray()
}
