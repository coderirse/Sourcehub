package com.example.sourcehub.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sourcehub.SourcehubApplication
import com.example.sourcehub.presentation.common.state.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ForgotPasswordUiState(
    val email: String = "",
    val isLoading: Boolean = false,
    val message: String? = null,
    val isSuccess: Boolean = false
)

class ForgotPasswordViewModel : ViewModel() {
    private val authRepository = SourcehubApplication.instance.appContainer.authRepository
    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    fun onEmailChange(email: String) { _uiState.update { it.copy(email = email, message = null) } }

    fun submit() {
        val email = _uiState.value.email
        if (email.isBlank()) {
            _uiState.update { it.copy(message = "请输入邮箱", isSuccess = false) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            when (val result = authRepository.forgotPassword(email)) {
                is Resource.Success -> _uiState.update { it.copy(isLoading = false, message = "重置邮件已发送，请查收", isSuccess = true) }
                is Resource.Error -> _uiState.update { it.copy(isLoading = false, message = result.message, isSuccess = false) }
                is Resource.Loading -> {}
            }
        }
    }
}
