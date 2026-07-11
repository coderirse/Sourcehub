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
    val cacheSize: String = "0 MB"
)

class SettingsViewModel : ViewModel() {
    private val prefsManager = SourcehubApplication.instance.appContainer.preferencesManager
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
    }

    fun toggleWifiOnly(enabled: Boolean) {
        viewModelScope.launch { prefsManager.setWifiOnlyDownload(enabled) }
    }

    fun toggleBiometricLock(enabled: Boolean) {
        viewModelScope.launch { prefsManager.setBiometricLock(enabled) }
    }

    fun clearCache() {
        _uiState.update { it.copy(cacheSize = "0 MB") }
    }
}
