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
