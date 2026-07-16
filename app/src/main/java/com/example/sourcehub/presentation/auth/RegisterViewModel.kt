package com.example.sourcehub.presentation.auth

/**
 * 注册页面界面状态和视图模型。
 *
 * 持有 [RegisterUiState] — 捕获注册页面所需的每个表单字段
 * 和瞬时状态的数据类 — 以及 [RegisterViewModel]，
 * 它执行字段验证并调用认证仓库创建新账户。
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
 * 注册页面的不可变界面状态。
 *
 * @property name 用户选择的显示名称/昵称。
 * @property email 用作登录标识符的邮箱地址。
 * @property password 选择的密码（表单中为明文）。
 * @property confirmPassword 确认输入；提交前必须与 [password] 匹配。
 * @property isLoading 注册请求进行中时为 `true`。
 * @property error 人类可读的验证或服务器错误，或 `null`。
 */
data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * 驱动注册页面的视图模型。
 *
 * 执行三步客户端验证：
 * 1. 所有必填字段必须非空。
 * 2. 密码必须至少 6 个字符。
 * 3. 密码与确认密码必须匹配。
 *
 * 验证通过后，委托账户创建给 [AuthRepository]。
 * 成功时发出 [UiEvent.Navigate] 事件；
 * 失败时错误消息通过 [RegisterUiState.error] 显示。
 */
class RegisterViewModel : ViewModel() {

    /** 从应用级 DI 容器获取的仓库句柄。 */
    private val authRepository = SourcehubApplication.instance.appContainer.authRepository

    /** 注册界面状态的可变支持字段。 */
    private val _uiState = MutableStateFlow(RegisterUiState())
    /** 供组合页面使用的只读 [StateFlow]。 */
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    /**
     * 用于一次性导航事件的缓冲 [Channel]。
     * [Channel.BUFFERED] 确保事件不会丢失。
     */
    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    /** 页面观察以响应导航触发器的 [Flow]。 */
    val events = _events.receiveAsFlow()

    /** 更新昵称字段并清除任何过期错误。 */
    fun onNameChange(name: String) { _uiState.update { it.copy(name = name, error = null) } }
    /** 更新邮箱字段并清除任何过期错误。 */
    fun onEmailChange(email: String) { _uiState.update { it.copy(email = email, error = null) } }
    /** 更新密码字段并清除任何过期错误。 */
    fun onPasswordChange(password: String) { _uiState.update { it.copy(password = password, error = null) } }
    /** 更新确认密码字段并清除任何过期错误。 */
    fun onConfirmPasswordChange(confirmPassword: String) { _uiState.update { it.copy(confirmPassword = confirmPassword, error = null) } }

    /**
     * 验证表单输入，如果通过则启动协程注册新账户。
     *
     * 验证关卡（按顺序检查）：
     * - 所有字段必须非空。
     * - 密码必须至少 6 个字符。
     * - 密码和确认密码必须完全相同。
     */
    fun register() {
        val state = _uiState.value

        // 1. 确保所有必填字段已填写
        if (state.name.isBlank() || state.email.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(error = "请填写所有字段") }
            return
        }
        // 2. 强制最小密码长度
        if (state.password.length < 6) {
            _uiState.update { it.copy(error = "密码至少6位") }
            return
        }
        // 3. 验证两次密码输入是否匹配
        if (state.password != state.confirmPassword) {
            _uiState.update { it.copy(error = "两次密码不一致") }
            return
        }

        // ---- 启动异步注册请求 ----
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
                is Resource.Loading -> {
                    // 中间加载状态；无需更新界面。
                }
            }
        }
    }
}
