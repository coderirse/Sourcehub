package com.example.sourcehub.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sourcehub.SourcehubApplication
import com.example.sourcehub.presentation.common.state.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditProfileUiState(
    val name: String = "测试用户",
    val email: String = "test@sourcehub.com",
    val phone: String = "138****8888",
    val message: String? = null,
    val isSuccess: Boolean = false
)

class EditProfileViewModel : ViewModel() {
    private val authRepository = SourcehubApplication.instance.appContainer.authRepository
    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    fun onNameChange(name: String) { _uiState.update { it.copy(name = name) } }
    fun onPhoneChange(phone: String) { _uiState.update { it.copy(phone = phone) } }

    fun save() {
        viewModelScope.launch {
            val state = _uiState.value
            when (val result = authRepository.updateProfile(
                com.example.sourcehub.domain.model.User(name = state.name, phone = state.phone)
            )) {
                is Resource.Success -> _uiState.update { it.copy(message = "保存成功", isSuccess = true) }
                is Resource.Error -> _uiState.update { it.copy(message = result.message, isSuccess = false) }
                is Resource.Loading -> {}
            }
        }
    }
}
