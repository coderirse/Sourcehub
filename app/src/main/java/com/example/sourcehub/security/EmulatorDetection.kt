package com.example.sourcehub.security

import android.os.Build
import java.io.File

object EmulatorDetection {
    fun isEmulator(): Boolean {
        return checkFingerprint() ||
                checkModel() ||
                checkManufacturer() ||
                checkHardware() ||
                checkEmulatorFiles()
    }

    private fun checkFingerprint(): Boolean {
        val fingerprint = Build.FINGERPRINT
        return fingerprint.startsWith("generic") ||
                fingerprint.startsWith("unknown") ||
                fingerprint.contains("generic") ||
                fingerprint.contains("vbox") ||
                fingerprint.contains("sdk_gphone")
    }

    private fun checkModel(): Boolean {
        val model = Build.MODEL.lowercase()
        return model.contains("google_sdk") ||
                model.contains("emulator") ||
                model.contains("android sdk built for x86") ||
                model.contains("virtual") ||
                model == "sdk" ||
                model == "sdk_x86"
    }

    private fun checkManufacturer(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return manufacturer.contains("genymotion") ||
                manufacturer.contains("virtual") ||
                manufacturer.contains("android")
    }

    private fun checkHardware(): Boolean {
        val hardware = Build.HARDWARE.lowercase()
        return hardware.contains("goldfish") ||
                hardware.contains("ranchu") ||
                hardware.contains("vbox86") ||
                hardware == "sdk"
    }

    private fun checkEmulatorFiles(): Boolean {
        val paths = arrayOf(
            "/dev/socket/qemud",
            "/dev/qemu_pipe",
            "/proc/tty/drivers",
            "/system/bin/qemu-props",
            "/init.goldfish.rc"
        )
        return paths.any { File(it).exists() }
    }
}
