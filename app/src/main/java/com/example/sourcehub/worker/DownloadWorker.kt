package com.example.sourcehub.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.sourcehub.SourcehubApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class DownloadWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val downloadId = inputData.getString("downloadId") ?: return@withContext Result.failure()
        val fileUrl = inputData.getString("fileUrl") ?: return@withContext Result.failure()
        val fileName = inputData.getString("fileName") ?: return@withContext Result.failure()

        val app = SourcehubApplication.instance
        val storageManager = app.appContainer.let {
            com.example.sourcehub.data.filestorage.FileStorageManager(applicationContext)
        }
        val cryptoManager = com.example.sourcehub.security.CryptoManager(applicationContext)

        setForeground(createForegroundInfo(fileName))

        try {
            val url = URL(fileUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connect()

            val totalBytes = connection.contentLengthLong
            val inputStream = connection.inputStream
            val tempFile = storageManager.getTempFilePath(fileName)
            val encryptedFile = storageManager.getEncryptedFilePath(fileName)

            FileOutputStream(tempFile).use { output ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalRead = 0L
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    totalRead += bytesRead
                    val progress = totalRead.toFloat() / totalBytes
                    setProgress(workDataOf("progress" to progress, "downloaded" to totalRead))
                }
            }

            // Encrypt the downloaded file
            cryptoManager.encryptFile(tempFile, encryptedFile)
            tempFile.delete()

            Result.success(
                workDataOf(
                    "localPath" to encryptedFile.absolutePath,
                    "fileSize" to totalBytes
                )
            )
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private fun createForegroundInfo(fileName: String): ForegroundInfo {
        val notification = androidx.core.app.NotificationCompat.Builder(applicationContext, "download_channel")
            .setContentTitle("下载中")
            .setContentText(fileName)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .build()
        return ForegroundInfo(1, notification)
    }
}
