package com.example.sourcehub.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sourcehub.SourcehubApplication
import com.example.sourcehub.presentation.common.state.Resource
import com.example.sourcehub.presentation.common.state.UiEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

class RegisterViewModel : ViewModel() {
    private val authRepository = SourcehubApplication.instance.appContainer.authRepository
    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()
    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onNameChange(name: String) { _uiState.update { it.copy(name = name, error = null) } }
    fun onEmailChange(email: String) { _uiState.update { it.copy(email = email, error = null) } }
    fun onPasswordChange(password: String) { _uiState.update { it.copy(password = password, error = null) } }
    fun onConfirmPasswordChange(confirmPassword: String) { _uiState.update { it.copy(confirmPassword = confirmPassword, error = null) } }

    fun register() {
        val state = _uiState.value
        if (state.name.isBlank() || state.email.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(error = "请填写所有字段") }
            return
        }
        if (state.password.length < 6) {
            _uiState.update { it.copy(error = "密码至少6位") }
            return
        }
        if (state.password != state.confirmPassword) {
            _uiState.update { it.copy(error = "两次密码不一致") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.register(state.name, state.email, state.password)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.send(UiEvent.Navigate("home"))
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                is Resource.Loading -> {}
            }
        }
    }
}
