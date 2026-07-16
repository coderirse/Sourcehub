/**
 * 一个 [CoroutineWorker]，负责从远程 URL 下载文件、加密下载内容并持久化结果到本地存储。
 *
 * ## 生命周期
 * 1. 从输入 [androidx.work.Data] 中读取 `downloadId`、`fileUrl` 和 `fileName`。
 * 2. 作为前台服务运行，即使应用进入后台，下载也会继续。
 * 3. 将 HTTP 响应流写入临时文件，通过 [setProgress] 报告进度。
 * 4. 下载完成后，通过 [CryptoManager] 加密临时文件并删除未加密的副本。
 * 5. 通过 [com.example.sourcehub.data.repository.DownloadRepository] 记录已完成的下载。
 * 6. 成功时返回 [androidx.work.ListenableWorker.Result.success] 并携带加密文件路径和文件大小，
 *    失败时最多重试三次。
 *
 * @property appContext  WorkManager 提供的应用 [Context]
 * @property params       包含输入数据和运行尝试次数的 [WorkerParameters]
 */
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

    /**
     * 在 [Dispatchers.IO] 上执行下载-加密-更新流水线。
     *
     * 这是 WorkManager 调用的入口。它会：
     * - 从 [inputData] 中提取必需参数（`downloadId`、`fileUrl`、`fileName`）。
     * - 打开 HTTP 连接并将响应体流式写入临时文件。
     * - 最多每 500 毫秒报告一次进度，使前台通知保持响应灵敏。
     * - 下载完成后加密临时文件并在仓库中记录成功。
     * - 失败时最多重试三次；第三次尝试后放弃。
     *
     * @return 携带 `localPath` 和 `fileSize` 的 [Result.success]，
     *         若未超尝试次数则返回 [Result.retry]，若应放弃该工作器则返回 [Result.failure]。
     */
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val downloadId = inputData.getString("downloadId") ?: return@withContext Result.failure()
        val fileUrl = inputData.getString("fileUrl") ?: return@withContext Result.failure()
        val fileName = inputData.getString("fileName") ?: "download"

        val app = SourcehubApplication.instance
        val storageManager = FileStorageManager(applicationContext)
        val cryptoManager = CryptoManager(applicationContext)
        val downloadRepo = app.appContainer.downloadRepository

        Log.i(TAG, "Starting download: $downloadId -> $fileName")

        // 将此工作器提升为前台服务，使用户离开应用时 Android 不会将其杀死。
        setForeground(createForegroundNotification(fileName))

        try {
            // Download to temp file
            val tempFile = storageManager.getTempFilePath(fileName)
            val encryptedFile = storageManager.getEncryptedFilePath(fileName)

            val url = URL(fileUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 30_000     // 30 秒用于 TCP / TLS 握手
            connection.readTimeout = 60_000        // 60 秒无活动后放弃
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "Download failed: HTTP ${connection.responseCode}")
                // 仅重试有限次数，避免在永久性错误（如 404）上无限循环。
                return@withContext if (runAttemptCount < 3) Result.retry() else Result.failure()
            }

            // 防范 -1（未知长度），强制至少为 1，使进度计算永远不会除以零。
            val totalBytes = connection.contentLengthLong.coerceAtLeast(1)
            val inputStream = connection.inputStream

            FileOutputStream(tempFile).use { output ->
                val buffer = ByteArray(8192)  // 8 KiB 流式缓冲区
                var bytesRead: Int
                var totalRead = 0L
                var lastProgressUpdate = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    totalRead += bytesRead

                    // 将进度更新节流至每 500 毫秒一次，避免过于频繁的 setProgress 调用
                    // 淹没 WorkManager。
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

            // 加密下载的文件，使其内容在磁盘上不可读。
            Log.i(TAG, "Encrypting: $downloadId")
            cryptoManager.encryptFile(tempFile, encryptedFile)
            tempFile.delete() // 删除未加密的临时文件——仅保留密文。

            // 更新本地数据库中的下载状态。
            downloadRepo.updateCompleted(downloadId, encryptedFile.absolutePath)

            Log.i(TAG, "Download+encrypt complete: $downloadId (${encryptedFile.length()} bytes)")
            Result.success(workDataOf("localPath" to encryptedFile.absolutePath, "fileSize" to totalBytes))
        } catch (e: Exception) {
            Log.e(TAG, "Download error: ${e.message}", e)
            // 在临时错误（网络波动、超时）时重试，但超过一定次数后放弃以避免电量消耗。
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    /**
     * 构建工作器活动时显示的 [ForegroundInfo] 通知。
     *
     * @param fileName  正在下载的文件的显示名称
     * @param progress  下载进度百分比（0 表示不确定 / "准备中"）
     * @return 携带下载进行中通知的 [ForegroundInfo]
     */
    private fun createForegroundNotification(fileName: String, progress: Int = 0): ForegroundInfo {
        val title = if (progress > 0) "下载中 $progress%" else "准备下载"
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(fileName)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            // progress == 0 时 indeterminate = true（旋转圆圈），否则为 false
            .setProgress(100, progress, progress == 0)
            .build()
        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    companion object {
        /** 此工作器中用于 [Log] 调用的标签。 */
        private const val TAG = "DownloadWorker"
        /** 用于下载进度通知的 Android 通知渠道 ID。 */
        private const val CHANNEL_ID = "download_channel"
        /** 唯一通知 ID，使此工作器不会与其他前台工作器冲突。 */
        private const val NOTIFICATION_ID = 1001
    }
}
