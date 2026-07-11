package com.example.sourcehub

import android.app.Application
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

        // Security checks (warning only in debug, enforcement in release)
        runSecurityChecks()

        // Initialize DI container
        appContainer = AppContainer(this)

        // Initialize WorkManager with custom config
        WorkManager.initialize(this, workManagerConfiguration)

        Log.i(TAG, "Sourcehub Application initialized")
    }

    private fun runSecurityChecks() {
        val isRooted = RootDetection.isDeviceRooted()
        val isEmulator = EmulatorDetection.isEmulator()
        val isDebugged = AntiDebugging.isDebuggerAttached()

        if (isRooted) {
            Log.w(TAG, "WARNING: Device appears to be rooted")
        }
        if (isEmulator) {
            Log.w(TAG, "WARNING: Running on emulator")
        }
        if (isDebugged) {
            Log.w(TAG, "WARNING: Debugger attached")
        }

        if (isRooted || isEmulator || isDebugged) {
            securityFlags = SecurityFlags(
                isRooted = isRooted,
                isEmulator = isEmulator,
                isDebugged = isDebugged
            )
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .build()

    data class SecurityFlags(
        val isRooted: Boolean = false,
        val isEmulator: Boolean = false,
        val isDebugged: Boolean = false
    )

    companion object {
        private const val TAG = "SourcehubApp"
        lateinit var instance: SourcehubApplication
            private set

        var securityFlags: SecurityFlags = SecurityFlags()
            private set
    }
}
