package com.example.sourcehub.data.local.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.sourcehub.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 基于 SQLiteOpenHelper 的数据库 — 真实 SQL，零注解处理。
 * 替代对 Room + KSP/KAPT 的需求。
 *
 * ## 数据库架构 (版本 1)
 *
 * ### `users` 表
 * 缓存当前登录用户的资料，使认证流程无需网络往返即可获得即时数据。
 * 一次只存储一个用户（活跃会话）。
 *
 * ### `cart_items` 表
 * 持久化每个用户的购物车项。每项以复合键 `(user_id, product_id)`
 * 进行去重查找。重复插入时增加数量而非创建新行。
 *
 * ### `downloads` 表
 * 跟踪文件下载任务，包括进度、状态和完成后的本地文件路径。
 * 持久化下载状态使暂停/恢复的下载在应用重启后仍然存在。
 *
 * ## 升级策略
 * [onUpgrade] 删除并重建所有表（预发布应用的最简方式）。
 * 在生产环境中应替换为增量迁移步骤。
 *
 * ## 线程安全
 * 所有公共方法通过 [withContext] 在 [Dispatchers.IO] 上运行，
 * 调用方可在任意协程作用域中调用而不会阻塞主线程。
 */
class SourcehubDbHelper(context: Context) : SQLiteOpenHelper(
    context, DB_NAME, null, DB_VERSION
) {
    override fun onCreate(db: SQLiteDatabase) {
        // ── users: 活跃会话的缓存用户资料 ──
        db.execSQL("""
            CREATE TABLE users (
                id TEXT PRIMARY KEY, name TEXT, email TEXT,
                avatar_url TEXT, phone TEXT, created_at INTEGER
            )
        """)
        // ── cart_items: 每用户购物车项 ──
        db.execSQL("""
            CREATE TABLE cart_items (
                id TEXT PRIMARY KEY, user_id TEXT, product_id TEXT,
                product_title TEXT, product_cover TEXT, price REAL,
                quantity INTEGER, added_at INTEGER
            )
        """)
        // ── downloads: 文件下载任务跟踪 ──
        db.execSQL("""
            CREATE TABLE downloads (
                id TEXT PRIMARY KEY, user_id TEXT, order_id TEXT,
                product_id TEXT, file_name TEXT, file_url TEXT,
                local_path TEXT, file_size INTEGER, downloaded_bytes INTEGER,
                status TEXT, file_type TEXT, created_at INTEGER
            )
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // 最简单的升级策略：全部删除再重建。
        // TODO: 在生产环境前替换为增量 ALTER TABLE 迁移。
        db.execSQL("DROP TABLE IF EXISTS users")
        db.execSQL("DROP TABLE IF EXISTS cart_items")
        db.execSQL("DROP TABLE IF EXISTS downloads")
        onCreate(db)
    }

    // ── 用户 CRUD ──

    /** 插入或替换用户记录（按主键 upsert）。 */
    suspend fun insertUser(user: User) = withContext(Dispatchers.IO) {
        writableDatabase.insertWithOnConflict("users", null, user.toValues(), SQLiteDatabase.CONFLICT_REPLACE)
    }

    /** 通过 ID 获取缓存的用户，未找到则返回 null。 */
    suspend fun getUser(id: String): User? = withContext(Dispatchers.IO) {
        readableDatabase.query("users", null, "id=?", arrayOf(id), null, null, null)
            .use { if (it.moveToFirst()) it.toUser() else null }
    }

    /** 删除所有缓存的用户记录（登出时调用）。 */
    suspend fun deleteAllUsers() = withContext(Dispatchers.IO) {
        writableDatabase.delete("users", null, null)
    }

    // ── 购物车 CRUD ──

    /** 插入新的购物车项，或当 ID 已存在时替换。 */
    suspend fun insertCartItem(item: CartItem) = withContext(Dispatchers.IO) {
        writableDatabase.insertWithOnConflict("cart_items", null, item.toValues(), SQLiteDatabase.CONFLICT_REPLACE)
    }

    /**
     * 获取用户的所有购物车项，按最近添加排序。
     * [CartRepositoryImpl] 使用此方法填充内存状态流。
     */
    suspend fun getCartItems(userId: String): List<CartItem> = withContext(Dispatchers.IO) {
        readableDatabase.query("cart_items", null, "user_id=?", arrayOf(userId), null, null, "added_at DESC")
            .use { mapCursor(it) { toCartItem() } }
    }

    /**
     * 按用户和商品查找购物车项。如果商品尚未加入购物车则返回 null —
     * 用于 [CartRepositoryImpl.addToCart] 中的去重。
     */
    suspend fun getCartItem(userId: String, productId: String): CartItem? = withContext(Dispatchers.IO) {
        readableDatabase.query("cart_items", null, "user_id=? AND product_id=?", arrayOf(userId, productId), null, null, null)
            .use { if (it.moveToFirst()) it.toCartItem() else null }
    }

    /** 更新购物车项行的数量。 */
    suspend fun updateCartQuantity(itemId: String, quantity: Int) = withContext(Dispatchers.IO) {
        writableDatabase.execSQL("UPDATE cart_items SET quantity=? WHERE id=?", arrayOf(quantity, itemId))
    }

    /** 通过行 ID 删除单个购物车项。 */
    suspend fun deleteCartItem(itemId: String) = withContext(Dispatchers.IO) {
        writableDatabase.delete("cart_items", "id=?", arrayOf(itemId))
    }

    /** 删除用户的所有购物车项。 */
    suspend fun clearCart(userId: String) = withContext(Dispatchers.IO) {
        writableDatabase.delete("cart_items", "user_id=?", arrayOf(userId))
    }

    /** 统计用户购物车中的商品数量（不计算总数）。 */
    suspend fun getCartCount(userId: String): Int = withContext(Dispatchers.IO) {
        readableDatabase.rawQuery("SELECT COUNT(*) FROM cart_items WHERE user_id=?", arrayOf(userId))
            .use { if (it.moveToFirst()) it.getInt(0) else 0 }
    }

    // ── 下载 CRUD ──

    /** 插入新的下载记录（按主键 upsert）。 */
    suspend fun insertDownload(d: Download) = withContext(Dispatchers.IO) {
        writableDatabase.insertWithOnConflict("downloads", null, d.toValues(), SQLiteDatabase.CONFLICT_REPLACE)
    }

    /**
     * 获取用户的所有下载任务，按最近创建排序。
     * [DownloadRepositoryImpl] 用于响应式下载列表。
     */
    suspend fun getDownloads(userId: String): List<Download> = withContext(Dispatchers.IO) {
        readableDatabase.query("downloads", null, "user_id=?", arrayOf(userId), null, null, "created_at DESC")
            .use { mapCursor(it) { toDownload() } }
    }

    /** 仅更新状态列（例如 已暂停、等待下载）。 */
    suspend fun updateDownloadStatus(id: String, status: String) = withContext(Dispatchers.IO) {
        writableDatabase.execSQL("UPDATE downloads SET status=? WHERE id=?", arrayOf(status, id))
    }

    /** 同时更新已下载字节数和状态（通常为下载中）。 */
    suspend fun updateDownloadProgress(id: String, bytes: Long, status: String) = withContext(Dispatchers.IO) {
        writableDatabase.execSQL("UPDATE downloads SET downloaded_bytes=?, status=? WHERE id=?", arrayOf(bytes, status, id))
    }

    /** 将下载标记为完成并记录本地文件路径。 */
    suspend fun markDownloadCompleted(id: String, path: String) = withContext(Dispatchers.IO) {
        writableDatabase.execSQL("UPDATE downloads SET local_path=?, status='COMPLETED' WHERE id=?", arrayOf(path, id))
    }

    /** 通过 ID 删除下载记录。 */
    suspend fun deleteDownload(id: String) = withContext(Dispatchers.IO) {
        writableDatabase.delete("downloads", "id=?", arrayOf(id))
    }

    companion object {
        private const val DB_NAME = "sourcehub.db"
        private const val DB_VERSION = 1
    }
}

// ── 扩展映射器：领域模型 <-> ContentValues / Cursor ──

/**
 * 将 [User] 领域模型转换为 [ContentValues] 以进行 SQLite 插入。
 * 列名使用 snake_case 与 DDL 保持一致。
 */
private fun User.toValues() = ContentValues().apply {
    put("id", id); put("name", name); put("email", email)
    put("avatar_url", avatarUrl); put("phone", phone); put("created_at", createdAt)
}

/** 从当前游标行读取 [User]。 */
private fun Cursor.toUser() = User(
    getString(getColumnIndexOrThrow("id")), getString(getColumnIndexOrThrow("name")),
    getString(getColumnIndexOrThrow("email")), getString(getColumnIndexOrThrow("avatar_url")),
    getString(getColumnIndexOrThrow("phone")), getLong(getColumnIndexOrThrow("created_at"))
)

/** 将 [CartItem] 领域模型转换为 [ContentValues]。 */
private fun CartItem.toValues() = ContentValues().apply {
    put("id", id); put("user_id", userId); put("product_id", productId)
    put("product_title", productTitle); put("product_cover", productCover)
    put("price", price); put("quantity", quantity); put("added_at", addedAt)
}

/** 从当前游标行读取 [CartItem]。 */
private fun Cursor.toCartItem() = CartItem(
    getString(getColumnIndexOrThrow("id")), getString(getColumnIndexOrThrow("user_id")),
    getString(getColumnIndexOrThrow("product_id")), getString(getColumnIndexOrThrow("product_title")),
    getString(getColumnIndexOrThrow("product_cover")), getDouble(getColumnIndexOrThrow("price")),
    getInt(getColumnIndexOrThrow("quantity")), getLong(getColumnIndexOrThrow("added_at"))
)

/**
 * 将 [Download] 领域模型转换为 [ContentValues]。
 * 枚举字段按名称存储为文本。
 */
private fun Download.toValues() = ContentValues().apply {
    put("id", id); put("user_id", userId); put("order_id", orderId)
    put("product_id", productId); put("file_name", fileName); put("file_url", fileUrl)
    put("local_path", localPath); put("file_size", fileSize); put("downloaded_bytes", downloadedBytes)
    put("status", status.name); put("file_type", fileType.name); put("created_at", createdAt)
}

/**
 * 从当前游标行读取 [Download]。
 *
 * [status] 和 [fileType] 列将枚举名称存储为文本。
 * 解析使用 [valueOf] 并以 [DownloadStatus.PENDING] /
 * [FileType.PDF] 作为损坏或未来枚举值的安全回退。
 */
private fun Cursor.toDownload() = Download(
    getString(getColumnIndexOrThrow("id")), getString(getColumnIndexOrThrow("user_id")),
    getString(getColumnIndexOrThrow("order_id")), getString(getColumnIndexOrThrow("product_id")),
    getString(getColumnIndexOrThrow("file_name")), getString(getColumnIndexOrThrow("file_url")),
    getString(getColumnIndexOrThrow("local_path")), getLong(getColumnIndexOrThrow("file_size")),
    getLong(getColumnIndexOrThrow("downloaded_bytes")),
    try { DownloadStatus.valueOf(getString(getColumnIndexOrThrow("status"))) } catch (e: Exception) { DownloadStatus.PENDING },
    try { FileType.valueOf(getString(getColumnIndexOrThrow("file_type"))) } catch (e: Exception) { FileType.PDF },
    getLong(getColumnIndexOrThrow("created_at"))
)

/**
 * 辅助函数：遍历游标并对每行应用 [mapper]，将结果收集为 [List]。
 * 游标不会由此函数关闭 — 调用方必须使用 `.use {}` 以确保清理。
 */
private inline fun <T> mapCursor(cursor: Cursor, mapper: Cursor.() -> T): List<T> {
    val list = mutableListOf<T>()
    while (cursor.moveToNext()) list.add(cursor.mapper())
    return list
}
