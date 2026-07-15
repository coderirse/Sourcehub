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

class SourcehubApplication : Application(), Configuration.Provider {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        runSecurityChecks()
        appContainer = AppContainer(this)
        createNotificationChannels()
        WorkManager.initialize(this, workManagerConfiguration)
        Log.i(TAG, "SourceHub Application initialized")
    }

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

    private fun runSecurityChecks() {
        val isRooted = RootDetection.isDeviceRooted()
        val isEmulator = EmulatorDetection.isEmulator()
        val isDebugged = AntiDebugging.isDebuggerAttached()
        if (isRooted) Log.w(TAG, "WARNING: Device appears to be rooted")
        if (isEmulator) Log.w(TAG, "WARNING: Running on emulator")
        if (isDebugged) Log.w(TAG, "WARNING: Debugger attached")
        securityFlags = SecurityFlags(isRooted, isEmulator, isDebugged)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setMinimumLoggingLevel(Log.INFO).build()

    data class SecurityFlags(val isRooted: Boolean = false, val isEmulator: Boolean = false, val isDebugged: Boolean = false)

    companion object {
        private const val TAG = "SourcehubApp"
        lateinit var instance: SourcehubApplication
            private set
        var securityFlags: SecurityFlags = SecurityFlags()
    }
}
