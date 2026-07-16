package com.example.sourcehub.presentation.common.state

/**
 * 表示异步 API 操作结果的密封类。
 *
 * 这是展示层用于统一渲染加载中、成功和错误状态的规范结果包装器。
 * 调用方通过模式匹配三个子类型来生成正确的 Compose 界面分支。
 *
 * @param T 成功载荷的类型。
 */
sealed class Resource<out T> {

    /**
     * 操作仍在进行中。
     *
     * 使用此状态显示加载指示器（旋转动画、骨架屏等）。
     * 因为它不携带数据，其类型参数固定为 [Nothing]，
     * 因此可以在多态场景下使用，不受期望的 [T] 类型限制。
     */
    data object Loading : Resource<Nothing>()

    /**
     * 操作已成功完成。
     *
     * @param data 操作返回的载荷。
     */
    data class Success<T>(val data: T) : Resource<T>()

    /**
     * 操作失败。
     *
     * @param message 人类可读的失败描述，适合在 Snackbar 或错误页面中展示。
     * @param throwable 可选的底层异常，保留用于日志记录或调试目的。通常不会展示给用户。
     */
    data class Error(val message: String, val throwable: Throwable? = null) : Resource<Nothing>()
}
