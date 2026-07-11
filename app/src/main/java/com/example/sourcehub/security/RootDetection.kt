package com.example.sourcehub.security

import android.os.Build
import java.io.File

object RootDetection {
    fun isDeviceRooted(): Boolean {
        return checkBuildTags() ||
                checkRootBinaries() ||
                checkRootManagementApps() ||
                checkSystemProperties() ||
                checkSuCommand()
    }

    private fun checkBuildTags(): Boolean {
        val buildTags = Build.TAGS
        return buildTags != null && buildTags.contains("test-keys")
    }

    private fun checkRootBinaries(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su"
        )
        return paths.any { File(it).exists() }
    }

    private fun checkRootManagementApps(): Boolean {
        val rootApps = arrayOf(
            "com.topjohnwu.magisk",
            "com.noshufou.android.su",
            "com.thirdparty.superuser",
            "eu.chainfire.supersu",
            "com.koushikdutta.superuser",
            "me.phh.superuser"
        )
        return try {
            val pm = Runtime.getRuntime().exec("pm list packages").inputStream.bufferedReader().readText()
            rootApps.any { pm.contains(it) }
        } catch (e: Exception) {
            false
        }
    }

    private fun checkSystemProperties(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("getprop ro.debuggable")
            val result = process.inputStream.bufferedReader().readText().trim()
            result == "1"
        } catch (e: Exception) {
            false
        }
    }

    private fun checkSuCommand(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("which", "su"))
            process.waitFor()
            process.inputStream.bufferedReader().readText().isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
}
