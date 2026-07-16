package com.example.sourcehub.presentation.common.state

/**
 * 视图模型用于驱动 Compose 页面的通用界面状态持有者。
 *
 * 与 [Resource]（和类型）不同，[UiState] 是单个数据类，
 * 同时追踪三个正交的状态维度——数据、加载中和错误。
 * 当需要在刷新进行中展示旧数据，或同时展示数据和内联错误时，这种方式非常有用。
 *
 * 派生属性 [isSuccess] 和 [isEmpty] 为两种最常见的稳定状态提供了便捷检查。
 *
 * @param T  数据载荷的类型（例如领域模型或列表）。
 * @param data  最近一次成功的结果，首次加载前为 `null`。
 * @param isLoading  当前是否有异步操作正在进行中。
 * @param error  人类可读的错误消息，无错误时为 `null`。
 */
data class UiState<T>(
    val data: T? = null,
    val isLoading: Boolean = false,
    val error: String? = null
) {
    /**
     * 当数据可用且既无加载中也无错误时返回 `true`。
     */
    val isSuccess: Boolean get() = data != null && !isLoading && error == null

    /**
     * 当页面尚未有数据且当前不在加载中或错误状态时返回 `true`。
     * 这通常表示需要进行首次加载，或收到了空列表结果。
     */
    val isEmpty: Boolean get() = data == null && !isLoading && error == null
}

/**
 * 视图模型希望传递给 Compose 页面的一次性界面事件。
 *
 * 与 [UiState]（建模持久页面状态）不同，[UiEvent] 被消费一次后即丢弃——
 * 通常通过 `Channel` 或 `SharedFlow` 在 `LaunchedEffect` 块中收集。
 * 这避免了在重组或配置变更时重复投递。
 */
sealed class UiEvent {
    /**
     * 指示页面导航到给定的 [route]。
     */
    data class Navigate(val route: String) : UiEvent()

    /**
     * 指示页面显示带有给定 [message] 的 [Snackbar](https://m3.material.io/components/snackbar)。
     */
    data class ShowSnackbar(val message: String) : UiEvent()

    /**
     * 指示页面返回（弹出返回栈）。
     */
    data object NavigateBack : UiEvent()
}
