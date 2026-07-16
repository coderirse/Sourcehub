/**
 * **文件下载路由：访问控制和文件服务**
 *
 * 本包定义了 `/api/files/*` 路由树。它是已购买数字产品的守门人 ——
 * 用户必须拥有包含该产品的已支付订单才能下载。
 *
 * ## 下载流程（两步）
 * 1. **POST /api/files/download-url/{productId}** -- 客户端请求下载 URL。
 *    服务器验证用户已购买并支付了该产品，创建 [Downloads] 审计记录，
 *    并返回带有随机令牌的限时下载 URL。
 * 2. **GET /api/files/download/{downloadId}?token=xxx** -- 客户端访问该 URL。
 *    服务器查找下载记录，找到关联的产品文件，并将其作为文件下载响应进行流式传输。
 *
 * ## 设计决策
 * - **两步流程**：将授权（第 1 步）与文件服务（第 2 步）分离，
 *   允许授权步骤扩展额外的检查（速率限制、并发下载上限、设备限制），
 *   而无需触及文件服务路径。
 * - **令牌参数**：`?token=` 查询参数已生成，但当前实现中**未验证**。
 *   在生产环境中，令牌应随下载记录（哈希后）存储，在 GET 请求中验证，
 *   并在单次使用后失效（一次性下载链接）。
 * - **文件存储**：文件从本地文件系统（`config.uploadDir`）提供服务。
 *   在生产环境中，迁移到对象存储（S3、MinIO）并使用预签名 URL 或代理流式传输。
 *
 * ## 回退行为
 * 如果产品的实际文件在磁盘上不存在（在使用种子数据开发时很常见），
 * 服务器会生成一个最小有效的 PDF（[generateSamplePdf]）并以此代替。
 * 这是一个**演示回退**，应在生产环境中移除。
 *
 * ## 生产就绪
 * - **令牌验证**：实现令牌验证和一次性使用。
 * - **对象存储**：用 S3/MinIO 预签名 URL 替换本地文件服务。
 *   这消除了通过服务器代理文件字节的需求。
 * - **范围请求**：支持 HTTP Range 头部以支持断点续传下载
 *   和视频/音频流式传输（未来的文件类型）。
 * - **下载限制**：使用 [Downloads] 审计表强制执行每用户下载限制
 *   （例如每个产品最多 5 次下载）。
 * - **病毒扫描**：在向用户提供文件之前扫描上传的文件。
 */
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

/**
 * 在 `/api/files` 下挂载所有文件下载路由，受 JWT 认证保护。
 *
 * @param jwtManager 用于提取当前用户 ID 的 JWT 令牌管理器。
 * @param db Exposed 数据库句柄。
 * @param config 应用程序配置（用于上传目录路径）。
 */
fun Routing.fileRoutes(jwtManager: JwtManager, db: Database, config: AppConfig) {
    authenticate("auth-jwt") {
        route("/api/files") {

            /**
             * POST /api/files/download-url/{productId}
             *
             * 为给定产品授权下载。验证已认证用户拥有包含此产品的
             * 已支付订单，然后生成下载记录并返回下载 URL。
             *
             * 路径参数：
             * - `productId`：要授权下载的产品。
             *
             * 状态码：
             * - 200：下载已授权；返回 URL。
             * - 401：未认证。
             * - 403：用户未购买此产品（或支付未完成）。
             * - 404：产品未找到。
             */
            post("/download-url/{productId}") {
                val userId = jwtManager.getUserId(call) ?: return@post call.respond(mapOf("code" to 401))
                val productId = call.parameters["productId"] ?: ""

                // 验证购买：用户必须至少有一个包含此产品的 PAID 订单。
                // 使用 OrderItems 和 Orders 之间的 inner join 以提高效率。
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

                // 为审计追踪生成下载记录，并为下载 URL 生成令牌。
                // 注意：令牌当前在 GET 端点上未验证 —— 参见文件级 KDoc 中的生产环境说明。
                val downloadId = "dl_${UUID.randomUUID().toString().take(8)}"
                val token = UUID.randomUUID().toString()
                val fileName = "${product[Products.title]}.${product[Products.fileType]?.lowercase() ?: "pdf"}"

                // 记录此下载请求以供审计。
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
                    "expiresAt" to (System.currentTimeMillis() + 3600000) // 1 小时后过期
                )))
            }

            /**
             * GET /api/files/download/{downloadId}?token=<token>
             *
             * 为先前已授权的下载提供实际文件字节。查找下载记录
             * 和关联的产品，然后流式传输文件。
             *
             * 如果产品的文件在磁盘上不存在，则动态生成一个最小有效的
             * PDF 作为回退（演示行为）。
             *
             * 路径参数：
             * - `downloadId`：来自第 1 步的下载记录 ID。
             *
             * 查询参数：
             * - `token`：来自第 1 步的下载令牌（当前未验证）。
             */
            get("/download/{downloadId}") {
                val downloadId = call.parameters["downloadId"] ?: ""
                val token = call.request.queryParameters["token"] ?: ""

                // 查找下载记录以确认这是一个有效请求。
                val download = newSuspendedTransaction(db = db) {
                    Downloads.select { Downloads.id eq downloadId }.singleOrNull()
                } ?: return@get call.respondText("Not found", status = HttpStatusCode.NotFound)

                val product = newSuspendedTransaction(db = db) {
                    Products.select { Products.id eq download[Downloads.productId] }.singleOrNull()
                } ?: return@get call.respondText("File not available", status = HttpStatusCode.NotFound)

                // 如果实际文件存在则提供之；否则回退到生成的示例 PDF
                //（仅开发环境 —— 在生产环境中移除）。
                val filePath = product[Products.filePath] ?: ""
                val file = File(config.uploadDir, filePath)

                if (file.exists()) {
                    call.response.header("Content-Disposition", "attachment; filename=\"${download[Downloads.fileName]}\"")
                    call.respondFile(file)
                } else {
                    // 回退：动态生成最小 PDF，以便演示流程在没有真实上传文件的情况下也能工作。
                    val sample = File(config.uploadDir, "sample.pdf")
                    if (!sample.exists()) sample.writeBytes(generateSamplePdf())
                    call.response.header("Content-Disposition", "attachment; filename=\"${download[Downloads.fileName]}\"")
                    call.respondFile(sample)
                }
            }
        }
    }
}

/**
 * 生成一个最小但结构有效的 PDF，作为演示目的的回退。
 * 这不是真正的文档 —— 它是一个空的单页 PDF。
 *
 * 在生产环境中，文件应由内容创作者上传并存储在对象存储中；
 * 此回退应完全移除。
 *
 * @return 包含有效 PDF 1.4 文件的字节数组。
 */
private fun generateSamplePdf(): ByteArray {
    // 最小有效 PDF 结构：catalog -> pages -> 单个空页面。
    // xref 表和 trailer 为此 4 对象结构手工制作。
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
