/**
 * 编辑资料页面的 ViewModel 层。
 *
 * 管理表单字段（昵称、邮箱、手机号），并通过
 * [AuthRepository.updateProfile] 将资料更新提交到后端。
 */
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

/**
 * 编辑资料页面的 UI 状态。
 *
 * @property name 用户的显示名称。
 * @property email 用户的邮箱地址（本页面为只读）。
 * @property phone 用户的手机号。
 * @property message 保存操作后可选的反馈消息。
 * @property isSuccess 上一次保存操作是否成功。
 */
data class EditProfileUiState(
    val name: String = "测试用户",
    val email: String = "test@sourcehub.com",
    val phone: String = "138****8888",
    val message: String? = null,
    val isSuccess: Boolean = false
)

/**
 * 持有表单状态并处理资料保存流程的 ViewModel。
 *
 * 调用 [save] 时，当前的 [name] 和 [phone] 被发送到
 * [AuthRepository.updateProfile]。结果反映在
 * [EditProfileUiState.message] 和 [EditProfileUiState.isSuccess] 中。
 */
class EditProfileViewModel : ViewModel() {
    /** 认证和资料操作的仓库。 */
    private val authRepository = SourcehubApplication.instance.appContainer.authRepository
    /** 单一 [uiState] 流的可变后备字段。 */
    private val _uiState = MutableStateFlow(EditProfileUiState())
    /** 由编辑资料页面消费的只读 [StateFlow]。 */
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    /** 更新 UI 状态中的昵称字段。 */
    fun onNameChange(name: String) { _uiState.update { it.copy(name = name) } }

    /** 更新 UI 状态中的手机号字段。 */
    fun onPhoneChange(phone: String) { _uiState.update { it.copy(phone = phone) } }

    /**
     * 将当前昵称和手机号持久化到后端。
     *
     * 来自 [AuthRepository.updateProfile] 的 [Resource] 结果
     * 决定反馈消息是成功还是错误。
     */
    fun save() {
        viewModelScope.launch {
            val state = _uiState.value
            when (val result = authRepository.updateProfile(
                com.example.sourcehub.domain.model.User(name = state.name, phone = state.phone)
            )) {
                is Resource.Success -> _uiState.update { it.copy(message = "保存成功", isSuccess = true) }
                is Resource.Error -> _uiState.update { it.copy(message = result.message, isSuccess = false) }
                // 加载状态不需要更新 UI — 表单保持可交互。
                is Resource.Loading -> {}
            }
        }
    }
}
