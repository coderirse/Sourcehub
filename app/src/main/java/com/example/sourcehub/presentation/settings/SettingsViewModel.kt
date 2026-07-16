/**
 * 设置页面的 ViewModel 层。
 *
 * 管理应用级偏好设置：仅WiFi下载、生物识别锁定、
 * 远程API开关和缓存大小。从 [PreferencesManager] 读取初始值，
 * 并将更改回传。
 */
package com.example.sourcehub.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sourcehub.SourcehubApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 设置页面的 UI 状态。
 *
 * @property wifiOnly 下载是否仅限于 WiFi。
 * @property biometricLock 敏感文件是否启用了生物识别认证。
 * @property cacheSize 人类可读的当前缓存大小字符串（例如 "12 MB"）。
 * @property useRemoteApi 应用是否与远程 Ktor 后端通信。
 */
data class SettingsUiState(
    val wifiOnly: Boolean = true,
    val biometricLock: Boolean = false,
    val cacheSize: String = "0 MB",
    val useRemoteApi: Boolean = false
)

/**
 * 观察 [PreferencesManager] 的偏好流并向 UI 暴露开关操作的 ViewModel。
 *
 * 注意：[useRemoteApi] 从两个来源收集 — [PreferencesManager] 用于持久化，
 * [AppContainer] 用于内存中的运行时标记 — 因此 UI 与实际运行时状态保持同步。
 */
class SettingsViewModel : ViewModel() {
    /** 应用实例，用于访问偏好设置和容器。 */
    private val app = SourcehubApplication.instance
    /** 持久化用户偏好设置的管理器。 */
    private val prefsManager = app.appContainer.preferencesManager
    /** 单一 [uiState] 流的可变后备字段。 */
    private val _uiState = MutableStateFlow(SettingsUiState())
    /** 由设置页面消费的只读 [StateFlow]。 */
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        // 观察仅WiFi下载偏好。
        viewModelScope.launch {
            prefsManager.wifiOnlyDownload.collect { value ->
                _uiState.update { state -> state.copy(wifiOnly = value) }
            }
        }
        // 观察生物识别锁定偏好。
        viewModelScope.launch {
            prefsManager.biometricLock.collect { value ->
                _uiState.update { state -> state.copy(biometricLock = value) }
            }
        }
        // 观察已持久化的远程API偏好。
        viewModelScope.launch {
            prefsManager.useRemoteApi.collect { value ->
                _uiState.update { state -> state.copy(useRemoteApi = value) }
            }
        }
        // 观察来自应用容器的实时运行时远程API标记。
        // 这确保开关反映实际的连接状态，
        // 即使在该 ViewModel 之外发生更改。
        viewModelScope.launch {
            app.appContainer.useRemoteApi.collect { value ->
                _uiState.update { state -> state.copy(useRemoteApi = value) }
            }
        }
    }

    /** 持久化仅WiFi下载偏好。 */
    fun toggleWifiOnly(enabled: Boolean) {
        viewModelScope.launch { prefsManager.setWifiOnlyDownload(enabled) }
    }

    /** 持久化生物识别锁定偏好。 */
    fun toggleBiometricLock(enabled: Boolean) {
        viewModelScope.launch { prefsManager.setBiometricLock(enabled) }
    }

    /**
     * 切换远程 API 模式。
     *
     * 同时更新持久化偏好和 [AppContainer] 上的内存运行时标记，
     * 以便应用其他部分立即感知到变更。
     */
    fun toggleRemoteApi(enabled: Boolean) {
        viewModelScope.launch {
            prefsManager.setUseRemoteApi(enabled)
            app.appContainer.toggleRemoteApi(enabled)
        }
    }

    /**
     * 清除应用缓存。
     *
     * 当前仅将显示的缓存大小重置为 "0 MB"。
     * 实际实现还应清除磁盘上的缓存文件。
     */
    fun clearCache() {
        _uiState.update { it.copy(cacheSize = "0 MB") }
    }
}
