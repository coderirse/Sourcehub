package com.example.sourcehub.presentation.auth

/**
 * 登录页面界面状态和视图模型。
 *
 * 定义 [LoginUiState] — 表示登录页面每种视觉状态的不可变数据类 —
 * 以及 [LoginViewModel]，它持有认证工作流：输入收集、客户端
 * 验证、仓库调用和导航信号发送。
 */

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sourcehub.SourcehubApplication
import com.example.sourcehub.presentation.common.state.Resource
import com.example.sourcehub.presentation.common.state.UiEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 登录页面的不可变界面状态。
 *
 * @property email 用户在邮箱字段中输入的邮箱地址。
 * @property password 用户在密码字段中输入的密码。
 * @property isLoading 认证请求进行中时为 `true`；
 *   在此状态下登录按钮显示加载旋转器并被禁用。
 * @property error 人类可读的错误消息，没有需要显示的错误时为 `null`。
 */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * 驱动登录页面的视图模型。
 *
 * 将 [uiState] 作为 [StateFlow] 暴露，使组合页面可以
 * 响应式地渲染当前表单状态。通过 [onEmailChange] / [onPasswordChange]
 * 处理文本字段变更，并在 [login] 中触发完整认证流程。
 *
 * 成功时通过 [events] 发出一次性 [UiEvent.Navigate]；
 * 失败时将错误消息写入 [LoginUiState.error]。
 */
class LoginViewModel : ViewModel() {

    /** 从应用级 DI 容器获取的仓库句柄。 */
    private val authRepository = SourcehubApplication.instance.appContainer.authRepository

    /** 登录界面状态的可变支持字段 — 绝不直接暴露。 */
    private val _uiState = MutableStateFlow(LoginUiState())
    /** 供组合页面使用的只读 [StateFlow]。 */
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /**
     * 用于传递一次性导航事件的缓冲 [Channel]。
     * [Channel.BUFFERED] 防止在没有收集器准备就绪时丢失事件。
     */
    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    /** 页面收集以响应导航信号的 [Flow]。 */
    val events = _events.receiveAsFlow()

    /** 更新界面状态中的邮箱字段并清除任何过期错误。 */
    fun onEmailChange(email: String) { _uiState.update { it.copy(email = email, error = null) } }
    /** 更新界面状态中的密码字段并清除任何过期错误。 */
    fun onPasswordChange(password: String) { _uiState.update { it.copy(password = password, error = null) } }

    /**
     * 验证表单，如果有效则启动协程进行认证。
     *
     * 协程将 [LoginUiState.isLoading] 设为 `true`，调用仓库，
     * 然后根据 [Resource] 结果发出导航事件或显示错误。
     */
    fun login() {
        // ---- 客户端验证 ----
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(error = "请输入邮箱和密码") }
            return
        }

        // ---- 启动异步认证请求 ----
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.login(state.email, state.password)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.send(UiEvent.Navigate("home"))
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                is Resource.Loading -> {
                    // 中间加载状态；无需更新界面。
                }
            }
        }
    }
}
