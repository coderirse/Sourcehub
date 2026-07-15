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

data class OfflineFilesUiState(
    val completedDownloads: List<Download> = emptyList(),
    val storageUsed: Long = 0L,
    val storageTotal: Long = 1024L * 1024 * 1024
)

class OfflineFilesViewModel : ViewModel() {
    private val app = SourcehubApplication.instance
    private val downloadRepository = app.appContainer.downloadRepository
    private val userId = app.appContainer.authRepository.getUserId()
    private val _uiState = MutableStateFlow(OfflineFilesUiState())
    val uiState: StateFlow<OfflineFilesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            downloadRepository.getDownloads(userId).collect { downloads ->
                val completed = downloads.filter { it.status == DownloadStatus.COMPLETED }
                _uiState.update {
                    it.copy(
                        completedDownloads = completed,
                        storageUsed = completed.sumOf { d -> d.fileSize }
                    )
                }
            }
        }
    }

    fun openFile(context: Context, downloadId: String) {
        val download = _uiState.value.completedDownloads.find { it.id == downloadId } ?: return
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val storageManager = FileStorageManager(context)
                    val cryptoManager = CryptoManager(context)

                    val encryptedFile = storageManager.getEncryptedFilePath(download.fileName)
                    val tempFile = storageManager.getTempFilePath(download.fileName)

                    if (encryptedFile.exists()) {
                        // Decrypt to temp
                        cryptoManager.decryptFile(encryptedFile, tempFile)

                        // Open with appropriate app
                        val mimeType = MimeTypeMap.getSingleton()
                            .getMimeTypeFromExtension(tempFile.extension.removePrefix("."))
                            ?: "application/octet-stream"

                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            tempFile
                        )

                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, mimeType)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }

                        context.startActivity(intent)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("OfflineFiles", "Failed to open file: ${e.message}")
            }
        }
    }

    fun deleteFile(downloadId: String) {
        viewModelScope.launch {
            downloadRepository.deleteDownload(downloadId)
        }
    }
}
