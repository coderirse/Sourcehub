package com.example.sourcehub.security

import android.os.Debug
import kotlinx.coroutines.*

object AntiDebugging {
    private var debugCheckJob: Job? = null

    fun isDebuggerAttached(): Boolean {
        return Debug.isDebuggerConnected() || Debug.waitingForDebugger()
    }

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

    fun stopMonitoring() {
        debugCheckJob?.cancel()
        debugCheckJob = null
    }

    private fun onDebuggerDetected() {
        // In production: clear tokens, force logout, or exit
        android.util.Log.w("AntiDebugging", "Debugger detected!")
    }

    private const val CHECK_INTERVAL_MS = 5000L
}
