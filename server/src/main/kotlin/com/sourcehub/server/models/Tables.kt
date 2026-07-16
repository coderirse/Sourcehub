package com.sourcehub.server.models

import org.jetbrains.exposed.sql.*

object Users : Table("users") {
    val id = varchar("id", 64)
    val name = varchar("name", 128)
    val email = varchar("email", 256)
    val passwordHash = varchar("password_hash", 256)
    val avatarUrl = varchar("avatar_url", 512).default("")
    val phone = varchar("phone", 32).default("")
    val createdAt = long("created_at")
    override val primaryKey = PrimaryKey(id)
}

object Products : Table("products") {
    val id = varchar("id", 64)
    val title = varchar("title", 256)
    val description = text("description")
    val author = varchar("author", 128)
    val price = double("price")
    val originalPrice = double("original_price")
    val coverUrl = varchar("cover_url", 512).default("")
    val filePath = varchar("file_path", 512).default("")
    val fileType = varchar("file_type", 16).default("PDF")
    val pageCount = integer("page_count").default(0)
    val fileSize = long("file_size").default(0)
    val category = varchar("category", 64).default("")
    val salesCount = integer("sales_count").default(0)
    val rating = float("rating").default(0f)
    val tags = varchar("tags", 512).default("")
    val createdAt = long("created_at")
    override val primaryKey = PrimaryKey(id)
}

object Orders : Table("orders") {
    val id = varchar("id", 64)
    val userId = varchar("user_id", 64) references Users.id
    val totalAmount = double("total_amount")
    val discountAmount = double("discount_amount").default(0.0)
    val finalAmount = double("final_amount")
    val status = varchar("status", 32).default("PENDING")
    val paymentMethod = varchar("payment_method", 32).default("WECHAT")
    val transactionId = varchar("transaction_id", 128).default("")
    val couponCode = varchar("coupon_code", 64).default("")
    val createdAt = long("created_at")
    val paidAt = long("paid_at").default(0)
    override val primaryKey = PrimaryKey(id)
}

object OrderItems : Table("order_items") {
    val id = varchar("id", 64)
    val orderId = varchar("order_id", 64) references Orders.id
    val productId = varchar("product_id", 64)
    val productTitle = varchar("product_title", 256)
    val unitPrice = double("unit_price")
    val quantity = integer("quantity")
    override val primaryKey = PrimaryKey(id)
}

object Downloads : Table("downloads") {
    val id = varchar("id", 64)
    val userId = varchar("user_id", 64)
    val orderId = varchar("order_id", 64)
    val productId = varchar("product_id", 64)
    val fileName = varchar("file_name", 256)
    val createdAt = long("created_at")
    override val primaryKey = PrimaryKey(id)
}
