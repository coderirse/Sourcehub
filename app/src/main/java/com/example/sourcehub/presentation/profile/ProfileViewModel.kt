/**
 * 个人资料页面的 ViewModel 层。
 *
 * 提供反映当前已认证用户的 [ProfileUiState]，
 * 并维护订单/下载数量的占位数据。
 */
package com.example.sourcehub.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sourcehub.SourcehubApplication
import com.example.sourcehub.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 个人资料页面的 UI 状态。
 *
 * @property user 当前已认证的用户，未登录时为 `null`。
 * @property orderCount 用户订单总数的占位值。
 * @property downloadCount 用户下载总数的占位值。
 */
data class ProfileUiState(
    val user: User? = null,
    val orderCount: Int = 0,
    val downloadCount: Int = 0
)

/**
 * 通过 [uiState] 暴露当前已认证用户的 ViewModel。
 *
 * 初始化时开始从 [AuthRepository.getCurrentUser] 收集数据，
 * 并将每次发出的 [User] 推送到 [ProfileUiState] 中。
 */
class ProfileViewModel : ViewModel() {
    /** 应用级依赖容器。 */
    private val appContainer = SourcehubApplication.instance.appContainer
    /** 单一 [uiState] 流的可变后备字段。 */
    private val _uiState = MutableStateFlow(ProfileUiState())
    /** 由个人资料页面消费的只读 [StateFlow]。 */
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        // 观察认证仓库中的当前用户并更新 UI 状态。
        viewModelScope.launch {
            appContainer.authRepository.getCurrentUser().collect { user ->
                _uiState.update { it.copy(user = user) }
            }
        }
    }
}
