package com.example.sourcehub.presentation.download

import android.content.Context
import android.content.Intent
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sourcehub.SourcehubApplication
import com.example.sourcehub.data.filestorage.FileStorageManager
import com.example.sourcehub.domain.model.Download
import com.example.sourcehub.domain.model.DownloadStatus
import com.example.sourcehub.security.CryptoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 离线文件页面的 UI 状态。
 *
 * @property completedDownloads 状态为 [DownloadStatus.COMPLETED] 的已完成下载列表。
 * @property storageUsed 已使用的存储空间（字节），由所有已完成下载的文件大小求和得出。
 * @property storageTotal 总存储空间上限（字节），默认为 1GB。
 */
data class OfflineFilesUiState(
    val completedDownloads: List<Download> = emptyList(),
    val storageUsed: Long = 0L,
    val storageTotal: Long = 1024L * 1024 * 1024
)

/**
 * 离线文件页面的 ViewModel。
 *
 * 管理已完成下载的离线文件列表，提供文件存储空间统计、文件打开和文件删除功能。
 *
 * 初始化时订阅 [DownloadRepository.getDownloads]，从中过滤出 [DownloadStatus.COMPLETED]
 * 的下载项作为离线文件列表，并实时计算已用存储空间。
 *
 * 文件打开流程（必须在 IO 线程中执行）：
 * 1. 通过 [FileStorageManager] 获取加密文件的路径和临时文件路径。
 * 2. 使用 [CryptoManager.decryptFile] 将加密文件解密到临时路径。
 * 3. 通过 [MimeTypeMap] 解析文件扩展名对应的 MIME 类型。
 * 4. 使用 [FileProvider] 生成临时文件的 content URI。
 * 5. 构造 [Intent.ACTION_VIEW] 交由系统选择合适的应用打开。
 */
class OfflineFilesViewModel : ViewModel() {
    /** 应用实例，持有全局依赖容器。 */
    private val app = SourcehubApplication.instance
    /** 下载仓库。 */
    private val downloadRepository = app.appContainer.downloadRepository
    /** 当前登录用户的 ID。 */
    private val userId = app.appContainer.authRepository.getUserId()

    /** 内部可变状态流。 */
    private val _uiState = MutableStateFlow(OfflineFilesUiState())
    /** 向 UI 暴露的只读状态流。 */
    val uiState: StateFlow<OfflineFilesUiState> = _uiState.asStateFlow()

    init {
        // 订阅下载列表，每次仓库数据变化时重新过滤已完成项并计算存储空间
        viewModelScope.launch {
            downloadRepository.getDownloads(userId).collect { downloads ->
                // 离线文件仅包含已完成的下载
                val completed = downloads.filter { it.status == DownloadStatus.COMPLETED }
                _uiState.update {
                    it.copy(
                        completedDownloads = completed,
                        // 已用存储等于所有已完成文件大小的总和
                        storageUsed = completed.sumOf { d -> d.fileSize }
                    )
                }
            }
        }
    }

    /**
     * 打开指定的已完成下载文件。
     *
     * 文件在磁盘上以加密形式存储，打开前需要先解密到临时路径，
     * 然后通过系统 [Intent] 交由对应的应用打开（如 PDF 阅读器）。
     *
     * @param context Android [Context]，用于 [FileStorageManager]、[CryptoManager]
     *                和 [FileProvider] 的初始化以及启动 Intent。
     * @param downloadId 要打开的下载任务 ID。
     */
    fun openFile(context: Context, downloadId: String) {
        // 从当前状态中找到对应的下载项，找不到则直接返回
        val download = _uiState.value.completedDownloads.find { it.id == downloadId } ?: return
        viewModelScope.launch {
            try {
                // 文件 I/O 和加解密操作必须在 IO 调度器上执行
                withContext(Dispatchers.IO) {
                    val storageManager = FileStorageManager(context)
                    val cryptoManager = CryptoManager(context)

                    // 获取加密文件路径和临时解密文件路径
                    val encryptedFile = storageManager.getEncryptedFilePath(download.fileName)
                    val tempFile = storageManager.getTempFilePath(download.fileName)

                    // 仅当加密文件存在时才执行后续操作
                    if (encryptedFile.exists()) {
                        // 将加密文件解密到临时路径，便于外部应用访问
                        cryptoManager.decryptFile(encryptedFile, tempFile)

                        // 根据文件扩展名获取对应的 MIME 类型
                        val mimeType = MimeTypeMap.getSingleton()
                            .getMimeTypeFromExtension(tempFile.extension.removePrefix("."))
                            ?: "application/octet-stream"

                        // 通过 FileProvider 生成 content URI，避免 file:// URI 在 Android 7.0+ 上的安全限制
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            tempFile
                        )

                        // 构造 VIEW Intent，由系统选择合适应用打开文件
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, mimeType)
                            // 授予目标应用临时读取 URI 的权限
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            // 在独立任务栈中启动，避免影响当前应用导航
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }

                        context.startActivity(intent)
                    }
                }
            } catch (e: Exception) {
                // 文件打开失败时记录日志，不向用户抛出异常
                android.util.Log.e("OfflineFiles", "Failed to open file: ${e.message}")
            }
        }
    }

    /**
     * 删除指定的下载任务及其对应的离线文件。
     *
     * @param downloadId 要删除的下载任务 ID。
     */
    fun deleteFile(downloadId: String) {
        viewModelScope.launch {
            downloadRepository.deleteDownload(downloadId)
        }
    }
}
