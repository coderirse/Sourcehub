package com.example.sourcehub.worker

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.example.sourcehub.SourcehubApplication
import com.example.sourcehub.data.filestorage.FileStorageManager
import com.example.sourcehub.security.CryptoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

class DownloadWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val downloadId = inputData.getString("downloadId") ?: return@withContext Result.failure()
        val fileUrl = inputData.getString("fileUrl") ?: return@withContext Result.failure()
        val fileName = inputData.getString("fileName") ?: "download"

        val app = SourcehubApplication.instance
        val storageManager = FileStorageManager(applicationContext)
        val cryptoManager = CryptoManager(applicationContext)
        val downloadRepo = app.appContainer.downloadRepository

        Log.i(TAG, "Starting download: $downloadId -> $fileName")

        setForeground(createForegroundNotification(fileName))

        try {
            // Download to temp file
            val tempFile = storageManager.getTempFilePath(fileName)
            val encryptedFile = storageManager.getEncryptedFilePath(fileName)

            val url = URL(fileUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 30000
            connection.readTimeout = 60000
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "Download failed: HTTP ${connection.responseCode}")
                return@withContext if (runAttemptCount < 3) Result.retry() else Result.failure()
            }

            val totalBytes = connection.contentLengthLong.coerceAtLeast(1)
            val inputStream = connection.inputStream

            FileOutputStream(tempFile).use { output ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalRead = 0L
                var lastProgressUpdate = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    totalRead += bytesRead

                    // Update progress every 500ms
                    val now = System.currentTimeMillis()
                    if (now - lastProgressUpdate > 500) {
                        val progress = (totalRead.toFloat() / totalBytes * 100).toInt()
                        setProgress(workDataOf("progress" to progress, "downloaded" to totalRead))
                        setForeground(createForegroundNotification(fileName, progress))
                        lastProgressUpdate = now
                    }
                }
            }
            inputStream.close()

            // Encrypt downloaded file
            Log.i(TAG, "Encrypting: $downloadId")
            cryptoManager.encryptFile(tempFile, encryptedFile)
            tempFile.delete() // Remove unencrypted temp

            // Update download status
            downloadRepo.updateCompleted(downloadId, encryptedFile.absolutePath)

            Log.i(TAG, "Download+encrypt complete: $downloadId (${encryptedFile.length()} bytes)")
            Result.success(workDataOf("localPath" to encryptedFile.absolutePath, "fileSize" to totalBytes))
        } catch (e: Exception) {
            Log.e(TAG, "Download error: ${e.message}", e)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private fun createForegroundNotification(fileName: String, progress: Int = 0): ForegroundInfo {
        val title = if (progress > 0) "下载中 $progress%" else "准备下载"
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(fileName)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setProgress(100, progress, progress == 0)
            .build()
        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val TAG = "DownloadWorker"
        private const val CHANNEL_ID = "download_channel"
        private const val NOTIFICATION_ID = 1001
    }
}
