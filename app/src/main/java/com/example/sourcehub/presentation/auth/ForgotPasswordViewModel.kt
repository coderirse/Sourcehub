package com.example.sourcehub.presentation.auth

/**
 * 忘记密码页面界面状态和视图模型。
 *
 * 包含 [ForgotPasswordUiState] — 一个轻量级数据类，跟踪
 * 邮箱字段、加载状态和反馈消息 — 以及
 * [ForgotPasswordViewModel]，它验证邮箱并将密码重置请求
 * 委托给仓库。
 */

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
 * 忘记密码页面的不可变界面状态。
 *
 * @property email 用户在表单中输入的邮箱地址。
 * @property isLoading 密码重置请求进行中时为 `true`。
 * @property message 显示给用户的反馈字符串 — 成功确认或错误描述。
 *   不需要显示消息时为 `null`。
 * @property isSuccess [message] 是否表示操作成功；
 *   控制渲染消息文本所使用的颜色。
 */
data class ForgotPasswordUiState(
    val email: String = "",
    val isLoading: Boolean = false,
    val message: String? = null,
    val isSuccess: Boolean = false
)

/**
 * 驱动忘记密码页面的视图模型。
 *
 * 验证邮箱字段非空，然后启动协程
 * 调用 [AuthRepository.forgotPassword]。结果反映在
 * [ForgotPasswordUiState.message] 和 [ForgotPasswordUiState.isSuccess]
 * 中，使组合组件可以渲染适当的反馈。
 */
class ForgotPasswordViewModel : ViewModel() {

    /** 从应用级 DI 容器获取的仓库句柄。 */
    private val authRepository = SourcehubApplication.instance.appContainer.authRepository

    /** 忘记密码界面状态的可变支持字段。 */
    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    /** 供组合页面使用的只读 [StateFlow]。 */
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    /** 更新邮箱字段并清除任何先前的反馈消息。 */
    fun onEmailChange(email: String) { _uiState.update { it.copy(email = email, message = null) } }

    /**
     * 验证邮箱并发送密码重置请求。
     *
     * 如果邮箱字段为空，立即显示错误消息（不发起网络调用）。
     * 否则启动协程，设置加载标志，调用仓库，
     * 并相应地填充反馈 [message] 和 [isSuccess] 字段。
     */
    fun submit() {
        val email = _uiState.value.email

        // 验证是否输入了邮箱地址
        if (email.isBlank()) {
            _uiState.update { it.copy(message = "请输入邮箱", isSuccess = false) }
            return
        }

        // ---- 启动异步密码重置请求 ----
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            when (val result = authRepository.forgotPassword(email)) {
                is Resource.Success -> _uiState.update {
                    it.copy(isLoading = false, message = "重置邮件已发送，请查收", isSuccess = true)
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isLoading = false, message = result.message, isSuccess = false)
                }
                is Resource.Loading -> {
                    // 中间加载状态；无需更新界面。
                }
            }
        }
    }
}
