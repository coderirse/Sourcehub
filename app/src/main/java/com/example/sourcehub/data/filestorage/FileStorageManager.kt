package com.example.sourcehub.data.filestorage

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class FileStorageManager(private val context: Context) {

    private val materialsDir: File
        get() = File(context.filesDir, "materials").also { it.mkdirs() }

    private val encryptedDir: File
        get() = File(materialsDir, "encrypted").also { it.mkdirs() }

    private val tempDir: File
        get() = File(materialsDir, "temp").also { it.mkdirs() }

    fun getEncryptedFilePath(fileName: String): File {
        return File(encryptedDir, "$fileName.enc")
    }

    fun getTempFilePath(fileName: String): File {
        return File(tempDir, fileName)
    }

    fun getStorageUsed(): Long {
        return materialsDir.walkTopDown().sumOf { it.length() }
    }

    fun getStorageAvailable(): Long {
        return context.filesDir.freeSpace
    }

    fun deleteFile(file: File): Boolean {
        return if (file.exists()) file.delete() else true
    }

    fun clearTempFiles() {
        tempDir.listFiles()?.forEach { it.delete() }
    }

    fun clearAllFiles() {
        materialsDir.deleteRecursively()
        materialsDir.mkdirs()
        encryptedDir.mkdirs()
        tempDir.mkdirs()
    }

    fun fileExists(fileName: String): Boolean {
        return getEncryptedFilePath(fileName).exists()
    }
}
