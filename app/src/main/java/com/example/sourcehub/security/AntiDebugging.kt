/**
 * SourceHub 应用的调试器附加检测。
 *
 * 此对象提供调试器状态的一次性检查和持续监控，
 * 以增加逆向工程和运行时篡改的难度。
 *
 * ## 检测机制
 * - [isDebuggerAttached] 检查 [android.os.Debug.isDebuggerConnected] 和
 *   [android.os.Debug.waitingForDebugger]。前者在 Java 调试器（JDWP）连接时
 *   返回 `true`；后者在应用等待调试器附加的短暂窗口期间
 *   （例如 `android:debuggable` 在启动时暂停进程后）返回 `true`。
 *
 * ## 持续监控
 * [installAntiDebugging] 启动一个协程，每隔 [CHECK_INTERVAL_MS] 毫秒
 * 轮询调试器状态。当检测到调试器时，调用 [onDebuggerDetected]——
 * 在生产构建中应清除敏感数据并退出。
 *
 * ## 局限性
 * - 基于 JDWP 的检查无法检测纯原生调试器（例如 `gdbserver`、
 *   非 JDWP 模式的 Frida）。这些需要在原生代码中使用基于 ptrace 的检查。
 * - 轮询间隔是响应性和电池影响之间的权衡。
 * - 有决心的攻击者可以 hook [android.os.Debug] 使其始终返回 `false`。
 */
package com.example.sourcehub.security

import android.os.Debug
import kotlinx.coroutines.*

/**
 * 调试器检测工具，支持可选的持续监控。
 *
 * 从 [SourcehubApplication.onCreate]（或任何长生命周期作用域）调用
 * [installAntiDebugging] 以启用轮询。调用 [stopMonitoring] 进行清理。
 */
object AntiDebugging {

    /** 当前正在运行的轮询协程的句柄，或 `null`。 */
    private var debugCheckJob: Job? = null

    /**
     * 如果 Java 调试器当前连接到此进程，或进程正在等待调试器附加，
     * 则返回 `true`。
     *
     * 这是一次性检查；使用 [installAntiDebugging] 进行持续监控。
     */
    fun isDebuggerAttached(): Boolean {
        return Debug.isDebuggerConnected() || Debug.waitingForDebugger()
    }

    /**
     * 启动一个协程，定期调用 [isDebuggerAttached] 并在发现调试器时
     * 调用 [onDebuggerDetected]。
     *
     * 如果之前的监控任务仍处于活跃状态，则在新任务启动前取消它——
     * 在任何时间最多只有一个监控协程。
     *
     * @param scope 用于启动轮询协程的 [CoroutineScope]。
     *              应为应用级作用域（例如 `ProcessLifecycleOwner` 或
     *              自定义的 `SupervisorJob`），以便在 Activity 重建时存活。
     */
    fun installAntiDebugging(scope: CoroutineScope) {
        debugCheckJob?.cancel()
        debugCheckJob = scope.launch {
            while (isActive) {
                if (isDebuggerAttached()) {
                    onDebuggerDetected()
                }
                delay(CHECK_INTERVAL_MS)
            }
        }
    }

    /**
     * 取消持续监控协程。即使没有监控处于活跃状态
     * 也可以安全调用（此时为无操作）。
     */
    fun stopMonitoring() {
        debugCheckJob?.cancel()
        debugCheckJob = null
    }

    /**
     * 在持续监控期间检测到调试器时调用。
     *
     * **在生产环境中**，应清除令牌存储、强制退出登录，并
     * 可选地调用 `Process.killProcess(Process.myPid())` 终止应用。
     * 当前实现仅为 MVP 开发阶段记录警告。
     */
    private fun onDebuggerDetected() {
        // 生产环境中：清除令牌、强制退出登录或退出
        android.util.Log.w("AntiDebugging", "Debugger detected!")
    }

    /** 轮询间隔（毫秒）。5 秒在响应性和电池消耗之间取得平衡。 */
    private const val CHECK_INTERVAL_MS = 5000L
}
