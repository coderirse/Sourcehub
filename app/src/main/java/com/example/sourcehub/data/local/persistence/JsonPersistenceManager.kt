package com.example.sourcehub.data.local.persistence

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * JSON-file-based persistence using Android's built-in org.json.
 * No Room, no KAPT, no KSP — zero annotation processing required.
 * Data is stored in app-private filesDir/persist/ as .json files.
 */
class JsonPersistenceManager(context: Context) {

    private val persistDir = File(context.filesDir, "persist").also { it.mkdirs() }

    suspend fun saveObject(key: String, json: JSONObject) = withContext(Dispatchers.IO) {
        File(persistDir, "$key.json").writeText(json.toString())
    }

    suspend fun loadObject(key: String): JSONObject? = withContext(Dispatchers.IO) {
        val file = File(persistDir, "$key.json")
        if (file.exists()) try { JSONObject(file.readText()) } catch (e: Exception) { null }
        else null
    }

    suspend fun saveArray(key: String, json: JSONArray) = withContext(Dispatchers.IO) {
        File(persistDir, "$key.json").writeText(json.toString())
    }

    suspend fun loadArray(key: String): JSONArray? = withContext(Dispatchers.IO) {
        val file = File(persistDir, "$key.json")
        if (file.exists()) try { JSONArray(file.readText()) } catch (e: Exception) { null }
        else null
    }

    suspend fun delete(key: String) = withContext(Dispatchers.IO) {
        File(persistDir, "$key.json").delete()
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        persistDir.listFiles()?.forEach { it.delete() }
    }
}

// Extension helpers for CartItem persistence
fun org.json.JSONObject.toCartItem() = com.example.sourcehub.domain.model.CartItem(
    id = getString("id"),
    userId = getString("userId"),
    productId = getString("productId"),
    productTitle = getString("productTitle"),
    productCover = getString("productCover"),
    price = getDouble("price"),
    quantity = getInt("quantity"),
    addedAt = getLong("addedAt")
)

fun com.example.sourcehub.domain.model.CartItem.toJson() = org.json.JSONObject().apply {
    put("id", id)
    put("userId", userId)
    put("productId", productId)
    put("productTitle", productTitle)
    put("productCover", productCover)
    put("price", price)
    put("quantity", quantity)
    put("addedAt", addedAt)
}

// Extension helpers for Download persistence
fun org.json.JSONObject.toDownload() = com.example.sourcehub.domain.model.Download(
    id = getString("id"),
    userId = getString("userId"),
    orderId = getString("orderId"),
    productId = getString("productId"),
    fileName = getString("fileName"),
    fileUrl = getString("fileUrl"),
    localPath = getString("localPath"),
    fileSize = getLong("fileSize"),
    downloadedBytes = getLong("downloadedBytes"),
    status = try { com.example.sourcehub.domain.model.DownloadStatus.valueOf(getString("status")) } catch (e: Exception) { com.example.sourcehub.domain.model.DownloadStatus.PENDING },
    fileType = try { com.example.sourcehub.domain.model.FileType.valueOf(getString("fileType")) } catch (e: Exception) { com.example.sourcehub.domain.model.FileType.PDF },
    createdAt = getLong("createdAt")
)

fun com.example.sourcehub.domain.model.Download.toJson() = org.json.JSONObject().apply {
    put("id", id)
    put("userId", userId)
    put("orderId", orderId)
    put("productId", productId)
    put("fileName", fileName)
    put("fileUrl", fileUrl)
    put("localPath", localPath)
    put("fileSize", fileSize)
    put("downloadedBytes", downloadedBytes)
    put("status", status.name)
    put("fileType", fileType.name)
    put("createdAt", createdAt)
}

// Extension helpers for User persistence
fun org.json.JSONObject.toUser() = com.example.sourcehub.domain.model.User(
    id = getString("id"),
    name = getString("name"),
    email = getString("email"),
    avatarUrl = getString("avatarUrl"),
    phone = getString("phone"),
    createdAt = getLong("createdAt")
)

fun com.example.sourcehub.domain.model.User.toJson() = org.json.JSONObject().apply {
    put("id", id)
    put("name", name)
    put("email", email)
    put("avatarUrl", avatarUrl)
    put("phone", phone)
    put("createdAt", createdAt)
}
