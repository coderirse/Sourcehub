/**
 * **产品目录路由：列表、详情、搜索、推荐**
 *
 * 本包定义了 `/api/products/*` 路由树。这些端点是**公开的** ——
 * 不需要 JWT 认证，因为浏览目录是购买前的活动。
 *
 * ## 端点
 * | 方法   | 路径                     | 描述                           |
 * |--------|-------------------------|--------------------------------|
 * | GET    | /api/products           | 带分类筛选的分页产品列表        |
 * | GET    | /api/products/recommended| 按销量排名的前 10 个产品       |
 * | GET    | /api/products/{id}      | 单个产品详情                   |
 * | GET    | /api/products/search?q= | 跨标题、作者、标签的全文搜索    |
 *
 * ## GET /api/products 的查询参数
 * - `page`（默认 1）：页码（从 1 开始）
 * - `size`（默认 20，最大 100）：每页条数
 * - `category`：按分类标识筛选
 * - `sort`：`"newest"`（默认）、`"sales"`、`"price_asc"`、`"price_desc"`
 *
 * ## 设计说明
 * - **无需认证**：产品是公开的。购买检查在 [FileRoutes] 的下载时进行。
 * - **通过 LIKE 搜索**：使用 SQL `LIKE '%term%'`，无法扩展到大型目录。
 *   在生产环境中，集成全文搜索引擎
 *   （PostgreSQL `tsvector`、Elasticsearch 或 MeiliSearch）。
 * - **标签存储为逗号分隔字符串**：简单但未规范化。
 *   在生产环境中，使用关联表（`product_tags`）以允许高效的
 *   基于标签的筛选，并避免 `LIKE` 的变通方法。
 * - **无缓存层**：每个请求都直接访问数据库。为不经常变化的产品列表
 *   添加缓存层（Redis 或 Ktor 内置缓存）。
 *
 * ## 生产就绪
 * - 在响应中添加**分页元数据**（总条数、总页数、
 *   hasNext/hasPrev），以便客户端可以渲染适当的分页控件。
 * - 为搜索端点添加**速率限制** —— 在大数据集上 LIKE 查询可能很昂贵。
 * - `productMap` 辅助函数在主列表处理器中内联重复。
 *   考虑提取一个通用的响应映射器。
 */
package com.sourcehub.server.routes

import com.sourcehub.server.models.Products
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

/**
 * 在 `/api/products` 下挂载所有产品目录路由。
 *
 * @param db 用于执行查询的 Exposed 数据库句柄。
 */
fun Routing.productRoutes(db: Database) {
    route("/api/products") {

        /**
         * GET /api/products
         *
         * 返回分页、筛选、排序的产品列表。
         * 支持通过查询参数进行分类筛选和多种排序方式。
         */
        get {
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            // 将每页条数限制在 1 到 100 之间以防止滥用。
            val size = (call.request.queryParameters["size"]?.toIntOrNull() ?: 20).coerceIn(1, 100)
            val category = call.request.queryParameters["category"]
            val sort = call.request.queryParameters["sort"] ?: "newest"
            // SQL LIMIT/OFFSET 使用基于 0 的偏移量；page 是基于 1 的。
            val offset = (page - 1) * size

            val rows = newSuspendedTransaction(db = db) {
                val base = Products.selectAll()
                // 应用可选的分类筛选。
                val filtered = if (category != null && category.isNotBlank())
                    base.where { Products.category eq category }
                else base

                // 应用排序。默认为最新优先。
                val sorted = when (sort) {
                    "sales" -> filtered.orderBy(Products.salesCount, SortOrder.DESC)
                    "price_asc" -> filtered.orderBy(Products.price, SortOrder.ASC)
                    "price_desc" -> filtered.orderBy(Products.price, SortOrder.DESC)
                    else -> filtered.orderBy(Products.createdAt, SortOrder.DESC)
                }
                // 应用分页并将行映射为响应 map。
                sorted.limit(size, offset.toLong()).map { row ->
                    mapOf(
                        "id" to row[Products.id], "title" to row[Products.title],
                        "description" to row[Products.description], "author" to row[Products.author],
                        "price" to row[Products.price], "originalPrice" to row[Products.originalPrice],
                        "coverUrl" to (row[Products.coverUrl] ?: ""),
                        "fileUrl" to (row[Products.filePath] ?: ""),
                        "fileType" to (row[Products.fileType] ?: "PDF"),
                        "pageCount" to row[Products.pageCount], "fileSize" to row[Products.fileSize],
                        "categoryId" to (row[Products.category] ?: ""),
                        "salesCount" to row[Products.salesCount], "rating" to row[Products.rating],
                        "tags" to (row[Products.tags] ?: "").split(",").filter { it.isNotBlank() },
                        "createdAt" to row[Products.createdAt]
                    )
                }
            }
            call.respond(mapOf("code" to 200, "data" to rows))
        }

        /**
         * GET /api/products/recommended
         *
         * 返回按销量降序排列的前 10 个产品。
         * 这是一个简单的基于热度的推荐 —— 在生产环境中
         * 替换为协同过滤或基于内容的推荐模型。
         */
        get("/recommended") {
            val rows = newSuspendedTransaction(db = db) {
                Products.selectAll().orderBy(Products.salesCount, SortOrder.DESC).limit(10).map { row ->
                    productMap(row)
                }
            }
            call.respond(mapOf("code" to 200, "data" to rows))
        }

        /**
         * GET /api/products/{id}
         *
         * 返回单个产品的完整详情。文件路径/URL 包含在响应中，
         * 但实际文件访问受 [FileRoutes] 中的购买检查控制。
         */
        get("/{id}") {
            val id = call.parameters["id"] ?: ""
            val row = newSuspendedTransaction(db = db) {
                Products.select { Products.id eq id }.singleOrNull()
            }
            if (row != null) {
                call.respond(mapOf("code" to 200, "data" to productMap(row)))
            } else {
                call.respond(mapOf("code" to 404, "message" to "Product not found"))
            }
        }

        /**
         * GET /api/products/search?q=<term>
         *
         * 通过将查询与标题、作者和标签进行匹配来搜索产品
         * （不区分大小写的 LIKE）。结果限制为 30 条。
         *
         * **规模限制**：SQL `LIKE '%term%'` 无法使用标准 B-tree 索引。
         * 在大规模下（>10k 产品）迁移到全文搜索。
         */
        get("/search") {
            val q = call.request.queryParameters["q"]?.lowercase() ?: ""
            val rows = newSuspendedTransaction(db = db) {
                Products.selectAll().where {
                    (Products.title.lowerCase() like "%$q%") or
                    (Products.author.lowerCase() like "%$q%") or
                    (Products.tags.lowerCase() like "%$q%")
                }.limit(30).map { row -> productMap(row) }
            }
            call.respond(mapOf("code" to 200, "data" to rows))
        }
    }
}

/**
 * 将 [Products] 结果行映射为一致的响应 map 结构。
 * 提取为私有辅助函数以避免在列表/详情端点之间重复。
 *
 * @param row 来自 [Products] 查询的结果行。
 * @return 适合 JSON 序列化的 `Map<String, Any?>`。
 */
private fun productMap(row: ResultRow) = mapOf(
    "id" to row[Products.id], "title" to row[Products.title],
    "description" to row[Products.description], "author" to row[Products.author],
    "price" to row[Products.price], "originalPrice" to row[Products.originalPrice],
    "coverUrl" to (row[Products.coverUrl] ?: ""), "fileUrl" to (row[Products.filePath] ?: ""),
    "fileType" to (row[Products.fileType] ?: "PDF"),
    "pageCount" to row[Products.pageCount], "fileSize" to row[Products.fileSize],
    "categoryId" to (row[Products.category] ?: ""), "salesCount" to row[Products.salesCount],
    "rating" to row[Products.rating],
    "tags" to (row[Products.tags] ?: "").split(",").filter { it.isNotBlank() },
    "createdAt" to row[Products.createdAt]
)
