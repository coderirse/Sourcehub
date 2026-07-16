package com.example.sourcehub.presentation.download

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sourcehub.SourcehubApplication
import com.example.sourcehub.domain.model.Download
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 下载列表页面的 UI 状态。
 *
 * @property downloads 当前所有下载任务列表，由仓库的 [Flow] 实时驱动更新。
 */
data class DownloadListUiState(val downloads: List<Download> = emptyList())

/**
 * 下载管理页面的 ViewModel。
 *
 * 订阅 [DownloadRepository] 中当前用户的所有下载任务，实时反映各下载项的状态变化。
 * 提供暂停、恢复、删除下载任务以及打开已完成文件的操作。
 *
 * 初始化时通过 [Flow.collect] 持续监听下载列表变化，确保 UI 与仓库状态保持同步。
 */
class DownloadListViewModel : ViewModel() {
    /** 应用级依赖容器。 */
    private val appContainer = SourcehubApplication.instance.appContainer
    /** 下载仓库，管理下载任务的增删改查。 */
    private val downloadRepository = appContainer.downloadRepository
    /** 当前登录用户的 ID。 */
    private val userId = appContainer.authRepository.getUserId()

    /** 内部可变状态流。 */
    private val _uiState = MutableStateFlow(DownloadListUiState())
    /** 向 UI 暴露的只读状态流。 */
    val uiState: StateFlow<DownloadListUiState> = _uiState.asStateFlow()

    init {
        // 初始化时订阅下载列表 Flow，每次仓库数据变化时自动更新 UI 状态
        viewModelScope.launch {
            downloadRepository.getDownloads(userId).collect { downloads ->
                _uiState.update { it.copy(downloads = downloads) }
            }
        }
    }

    /**
     * 暂停指定 ID 的下载任务。
     *
     * @param downloadId 要暂停的下载任务 ID。
     */
    fun pauseDownload(downloadId: String) {
        viewModelScope.launch { downloadRepository.pauseDownload(downloadId) }
    }

    /**
     * 恢复（继续）指定 ID 的下载任务。
     * 适用于 [DownloadStatus.PAUSED] 和 [DownloadStatus.FAILED] 状态的下载项。
     *
     * @param downloadId 要恢复的下载任务 ID。
     */
    fun resumeDownload(downloadId: String) {
        viewModelScope.launch { downloadRepository.resumeDownload(downloadId) }
    }

    /**
     * 删除指定 ID 的下载任务（包括其下载文件）。
     *
     * @param downloadId 要删除的下载任务 ID。
     */
    fun deleteDownload(downloadId: String) {
        viewModelScope.launch { downloadRepository.deleteDownload(downloadId) }
    }

    /**
     * 打开已完成下载的文件。
     * 当前为占位实现，生产环境中应解密文件并使用系统对应应用打开。
     *
     * @param downloadId 要打开的下载任务 ID。
     */
    fun openFile(downloadId: String) {
        // 生产环境中应解密文件并使用系统对应应用打开
    }
}
