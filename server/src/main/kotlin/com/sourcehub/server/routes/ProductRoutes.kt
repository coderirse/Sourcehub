package com.sourcehub.server.routes

import com.sourcehub.server.models.Products
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

fun Routing.productRoutes(db: Database) {
    route("/api/products") {

        get {
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val size = (call.request.queryParameters["size"]?.toIntOrNull() ?: 20).coerceIn(1, 100)
            val category = call.request.queryParameters["category"]
            val sort = call.request.queryParameters["sort"] ?: "newest"
            val offset = (page - 1) * size

            val rows = newSuspendedTransaction(db = db) {
                val base = Products.selectAll()
                val filtered = if (category != null && category.isNotBlank())
                    base.where { Products.category eq category }
                else base

                val sorted = when (sort) {
                    "sales" -> filtered.orderBy(Products.salesCount, SortOrder.DESC)
                    "price_asc" -> filtered.orderBy(Products.price, SortOrder.ASC)
                    "price_desc" -> filtered.orderBy(Products.price, SortOrder.DESC)
                    else -> filtered.orderBy(Products.createdAt, SortOrder.DESC)
                }
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

        get("/recommended") {
            val rows = newSuspendedTransaction(db = db) {
                Products.selectAll().orderBy(Products.salesCount, SortOrder.DESC).limit(10).map { row ->
                    productMap(row)
                }
            }
            call.respond(mapOf("code" to 200, "data" to rows))
        }

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

// Banners and categories (separate from products)
fun Routing.bannerCategoryRoutes(db: Database) {
    get("/api/banners") {
        val banners = listOf(
            mapOf("id" to "b1", "title" to "精品简历模板合集", "imageUrl" to "https://picsum.photos/800/300?random=1", "linkType" to "CATEGORY", "linkValue" to "cat_1", "sortOrder" to 0),
            mapOf("id" to "b2", "title" to "Python编程电子书", "imageUrl" to "https://picsum.photos/800/300?random=2", "linkType" to "PRODUCT", "linkValue" to "p7", "sortOrder" to 1),
            mapOf("id" to "b3", "title" to "年终总结PPT模板", "imageUrl" to "https://picsum.photos/800/300?random=3", "linkType" to "CATEGORY", "linkValue" to "cat_2", "sortOrder" to 2)
        )
        call.respond(mapOf("code" to 200, "data" to banners))
    }
    get("/api/categories") {
        val categories = listOf(
            mapOf("id" to "cat_1", "name" to "简历模板", "iconName" to "description", "sortOrder" to 0, "productCount" to 42),
            mapOf("id" to "cat_2", "name" to "PPT模板", "iconName" to "slideshow", "sortOrder" to 1, "productCount" to 38),
            mapOf("id" to "cat_3", "name" to "电子书", "iconName" to "book", "sortOrder" to 2, "productCount" to 56),
            mapOf("id" to "cat_4", "name" to "学习笔记", "iconName" to "school", "sortOrder" to 3, "productCount" to 23),
            mapOf("id" to "cat_5", "name" to "商业文档", "iconName" to "business", "sortOrder" to 4, "productCount" to 31),
            mapOf("id" to "cat_6", "name" to "技术文档", "iconName" to "code", "sortOrder" to 5, "productCount" to 19)
        )
        call.respond(mapOf("code" to 200, "data" to categories))
    }
}

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
