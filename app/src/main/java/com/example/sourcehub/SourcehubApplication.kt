/**
 * SourceHub Android 应用入口。
 *
 * 此 [Application] 子类拥有应用级生命周期，并作为以下各项的单一数据源：
 * - 依赖注入（通过 [AppContainer]）
 * - 安全状态评估（Root检测、模拟器检测、反调试检测）
 * - 通知渠道创建
 * - WorkManager 全局配置
 *
 * 伴生对象 [instance] 单例提供全局访问，使 Composable、ViewModel
 * 和其他框架无关代码无需依赖平台特定的依赖注入库即可访问 DI 容器。
 */
package com.example.sourcehub

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import androidx.work.Configuration
import androidx.work.WorkManager
import com.example.sourcehub.di.AppContainer
import com.example.sourcehub.security.AntiDebugging
import com.example.sourcehub.security.EmulatorDetection
import com.example.sourcehub.security.RootDetection

/**
 * 自定义 [Application] 类，在进程启动时初始化核心服务。
 *
 * 实现 [Configuration.Provider]，使 [WorkManager] 获得单一、一致的
 * 配置，无需在每个 Worker 中手动编写样板代码。
 */
class SourcehubApplication : Application(), Configuration.Provider {

    /**
     * 应用进程级别的依赖注入容器。
     * 在 [onCreate] 期间初始化，其后不再重新赋值。
     */
    lateinit var appContainer: AppContainer
        private set

    /**
     * 引导应用：运行安全检查、创建 DI 容器、注册
     * 通知渠道，并初始化 WorkManager。
     */
    override fun onCreate() {
        super.onCreate()
        instance = this

        // 在访问任何敏感数据之前运行威胁检测检查。
        runSecurityChecks()

        appContainer = AppContainer(this)

        // 设置下载进度通知渠道（API 26+ 必需）。
        createNotificationChannels()

        // 使用自定义日志级别初始化 WorkManager，确保 debug 构建
        // 在 logcat 中输出 Worker 诊断信息。
        WorkManager.initialize(this, workManagerConfiguration)

        Log.i(TAG, "SourceHub Application initialized")
    }

    /**
     * 创建 [DownloadWorker] 用于报告文件下载进度的 [NotificationChannel]。
     * 在 Android O 之前的设备上为无操作，因为通知渠道在那些版本上不存在。
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "download_channel",
                "下载通知",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "文件下载进度通知" }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * 运行三项核心安全检查（Root检测、模拟器检测、反调试检测），并将结果
     * 存储在伴生对象的 [securityFlags] 中。仅记录警告——各个屏幕可根据
     * 这些标志决定是否限制功能。
     */
    private fun runSecurityChecks() {
        val isRooted = RootDetection.isDeviceRooted()
        val isEmulator = EmulatorDetection.isEmulator()
        val isDebugged = AntiDebugging.isDebuggerAttached()

        // 记录警告日志，便于开发人员在 QA 期间检查安全状态。
        if (isRooted) Log.w(TAG, "WARNING: Device appears to be rooted")
        if (isEmulator) Log.w(TAG, "WARNING: Running on emulator")
        if (isDebugged) Log.w(TAG, "WARNING: Debugger attached")

        securityFlags = SecurityFlags(isRooted, isEmulator, isDebugged)
    }

    /**
     * 提供一个 [Configuration]，为所有 WorkManager Worker 设置最低日志级别。
     * 使用 `Log.INFO` 确保 Worker 生命周期事件在 logcat 中可见，
     * 同时避免过多详细的内部日志。
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setMinimumLoggingLevel(Log.INFO).build()

    /**
     * 应用启动时捕获的设备安全状态不可变快照。
     *
     * @property isRooted 当设备显示 Root 访问迹象时为 `true`
     *   （su 二进制文件、Magisk、test-keys 构建等）。
     * @property isEmulator 当设备指纹、硬件或文件系统
     *   痕迹表明处于模拟环境时为 `true`。
     * @property isDebugged 当调试器当前附加到此进程时为 `true`。
     */
    data class SecurityFlags(
        val isRooted: Boolean = false,
        val isEmulator: Boolean = false,
        val isDebugged: Boolean = false
    )

    companion object {
        private const val TAG = "SourcehubApp"

        /**
         * [SourcehubApplication] 实例的全局引用。
         * 在 `onCreate()` 之后安全访问——例如需要 DI 容器的 Composable。
         */
        lateinit var instance: SourcehubApplication
            private set

        /**
         * 在 [onCreate] 期间捕获的安全状态。屏幕和 ViewModel 可以读取此值
         * 以有条件地在已 Root 或模拟设备上禁用功能（如支付）。
         */
        var securityFlags: SecurityFlags = SecurityFlags()
    }
}
