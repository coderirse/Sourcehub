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
 * SQLiteOpenHelper-based database — real SQL, zero annotation processing.
 * Replaces the need for Room + KSP/KAPT.
 */
class SourcehubDbHelper(context: Context) : SQLiteOpenHelper(
    context, DB_NAME, null, DB_VERSION
) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE users (
                id TEXT PRIMARY KEY, name TEXT, email TEXT,
                avatar_url TEXT, phone TEXT, created_at INTEGER
            )
        """)
        db.execSQL("""
            CREATE TABLE cart_items (
                id TEXT PRIMARY KEY, user_id TEXT, product_id TEXT,
                product_title TEXT, product_cover TEXT, price REAL,
                quantity INTEGER, added_at INTEGER
            )
        """)
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
        db.execSQL("DROP TABLE IF EXISTS users")
        db.execSQL("DROP TABLE IF EXISTS cart_items")
        db.execSQL("DROP TABLE IF EXISTS downloads")
        onCreate(db)
    }

    // ── User CRUD ──

    suspend fun insertUser(user: User) = withContext(Dispatchers.IO) {
        writableDatabase.insertWithOnConflict("users", null, user.toValues(), SQLiteDatabase.CONFLICT_REPLACE)
    }

    suspend fun getUser(id: String): User? = withContext(Dispatchers.IO) {
        readableDatabase.query("users", null, "id=?", arrayOf(id), null, null, null)
            .use { if (it.moveToFirst()) it.toUser() else null }
    }

    suspend fun deleteAllUsers() = withContext(Dispatchers.IO) {
        writableDatabase.delete("users", null, null)
    }

    // ── Cart CRUD ──

    suspend fun insertCartItem(item: CartItem) = withContext(Dispatchers.IO) {
        writableDatabase.insertWithOnConflict("cart_items", null, item.toValues(), SQLiteDatabase.CONFLICT_REPLACE)
    }

    suspend fun getCartItems(userId: String): List<CartItem> = withContext(Dispatchers.IO) {
        readableDatabase.query("cart_items", null, "user_id=?", arrayOf(userId), null, null, "added_at DESC")
            .use { mapCursor(it) { toCartItem() } }
    }

    suspend fun getCartItem(userId: String, productId: String): CartItem? = withContext(Dispatchers.IO) {
        readableDatabase.query("cart_items", null, "user_id=? AND product_id=?", arrayOf(userId, productId), null, null, null)
            .use { if (it.moveToFirst()) it.toCartItem() else null }
    }

    suspend fun updateCartQuantity(itemId: String, quantity: Int) = withContext(Dispatchers.IO) {
        writableDatabase.execSQL("UPDATE cart_items SET quantity=? WHERE id=?", arrayOf(quantity, itemId))
    }

    suspend fun deleteCartItem(itemId: String) = withContext(Dispatchers.IO) {
        writableDatabase.delete("cart_items", "id=?", arrayOf(itemId))
    }

    suspend fun clearCart(userId: String) = withContext(Dispatchers.IO) {
        writableDatabase.delete("cart_items", "user_id=?", arrayOf(userId))
    }

    suspend fun getCartCount(userId: String): Int = withContext(Dispatchers.IO) {
        readableDatabase.rawQuery("SELECT COUNT(*) FROM cart_items WHERE user_id=?", arrayOf(userId))
            .use { if (it.moveToFirst()) it.getInt(0) else 0 }
    }

    // ── Download CRUD ──

    suspend fun insertDownload(d: Download) = withContext(Dispatchers.IO) {
        writableDatabase.insertWithOnConflict("downloads", null, d.toValues(), SQLiteDatabase.CONFLICT_REPLACE)
    }

    suspend fun getDownloads(userId: String): List<Download> = withContext(Dispatchers.IO) {
        readableDatabase.query("downloads", null, "user_id=?", arrayOf(userId), null, null, "created_at DESC")
            .use { mapCursor(it) { toDownload() } }
    }

    suspend fun updateDownloadStatus(id: String, status: String) = withContext(Dispatchers.IO) {
        writableDatabase.execSQL("UPDATE downloads SET status=? WHERE id=?", arrayOf(status, id))
    }

    suspend fun updateDownloadProgress(id: String, bytes: Long, status: String) = withContext(Dispatchers.IO) {
        writableDatabase.execSQL("UPDATE downloads SET downloaded_bytes=?, status=? WHERE id=?", arrayOf(bytes, status, id))
    }

    suspend fun markDownloadCompleted(id: String, path: String) = withContext(Dispatchers.IO) {
        writableDatabase.execSQL("UPDATE downloads SET local_path=?, status='COMPLETED' WHERE id=?", arrayOf(path, id))
    }

    suspend fun deleteDownload(id: String) = withContext(Dispatchers.IO) {
        writableDatabase.delete("downloads", "id=?", arrayOf(id))
    }

    companion object {
        private const val DB_NAME = "sourcehub.db"
        private const val DB_VERSION = 1
    }
}

// ── Extension mappers ──

private fun User.toValues() = ContentValues().apply {
    put("id", id); put("name", name); put("email", email)
    put("avatar_url", avatarUrl); put("phone", phone); put("created_at", createdAt)
}

private fun Cursor.toUser() = User(
    getString(getColumnIndexOrThrow("id")), getString(getColumnIndexOrThrow("name")),
    getString(getColumnIndexOrThrow("email")), getString(getColumnIndexOrThrow("avatar_url")),
    getString(getColumnIndexOrThrow("phone")), getLong(getColumnIndexOrThrow("created_at"))
)

private fun CartItem.toValues() = ContentValues().apply {
    put("id", id); put("user_id", userId); put("product_id", productId)
    put("product_title", productTitle); put("product_cover", productCover)
    put("price", price); put("quantity", quantity); put("added_at", addedAt)
}

private fun Cursor.toCartItem() = CartItem(
    getString(getColumnIndexOrThrow("id")), getString(getColumnIndexOrThrow("user_id")),
    getString(getColumnIndexOrThrow("product_id")), getString(getColumnIndexOrThrow("product_title")),
    getString(getColumnIndexOrThrow("product_cover")), getDouble(getColumnIndexOrThrow("price")),
    getInt(getColumnIndexOrThrow("quantity")), getLong(getColumnIndexOrThrow("added_at"))
)

private fun Download.toValues() = ContentValues().apply {
    put("id", id); put("user_id", userId); put("order_id", orderId)
    put("product_id", productId); put("file_name", fileName); put("file_url", fileUrl)
    put("local_path", localPath); put("file_size", fileSize); put("downloaded_bytes", downloadedBytes)
    put("status", status.name); put("file_type", fileType.name); put("created_at", createdAt)
}

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

private inline fun <T> mapCursor(cursor: Cursor, mapper: Cursor.() -> T): List<T> {
    val list = mutableListOf<T>()
    while (cursor.moveToNext()) list.add(cursor.mapper())
    return list
}
