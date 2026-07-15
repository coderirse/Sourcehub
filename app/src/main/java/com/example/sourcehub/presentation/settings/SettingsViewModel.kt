package com.example.sourcehub.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sourcehub.SourcehubApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val wifiOnly: Boolean = true,
    val biometricLock: Boolean = false,
    val cacheSize: String = "0 MB",
    val useRemoteApi: Boolean = false
)

class SettingsViewModel : ViewModel() {
    private val app = SourcehubApplication.instance
    private val prefsManager = app.appContainer.preferencesManager
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            prefsManager.wifiOnlyDownload.collect { value ->
                _uiState.update { state -> state.copy(wifiOnly = value) }
            }
        }
        viewModelScope.launch {
            prefsManager.biometricLock.collect { value ->
                _uiState.update { state -> state.copy(biometricLock = value) }
            }
        }
        viewModelScope.launch {
            prefsManager.useRemoteApi.collect { value ->
                _uiState.update { state -> state.copy(useRemoteApi = value) }
            }
        }
        viewModelScope.launch {
            app.appContainer.useRemoteApi.collect { value ->
                _uiState.update { state -> state.copy(useRemoteApi = value) }
            }
        }
    }

    fun toggleWifiOnly(enabled: Boolean) {
        viewModelScope.launch { prefsManager.setWifiOnlyDownload(enabled) }
    }

    fun toggleBiometricLock(enabled: Boolean) {
        viewModelScope.launch { prefsManager.setBiometricLock(enabled) }
    }

    fun toggleRemoteApi(enabled: Boolean) {
        viewModelScope.launch {
            prefsManager.setUseRemoteApi(enabled)
            app.appContainer.toggleRemoteApi(enabled)
        }
    }

    fun clearCache() {
        _uiState.update { it.copy(cacheSize = "0 MB") }
    }
}
