/**
 * Android 设备的 Root/越狱检测。
 *
 * 此对象采用分层检测策略——单一检查不足以确认，但多个独立信号的组合
 * 可以强有力地表明设备已被 Root。
 *
 * ## 检测层次
 * 1. **构建标签**——test-keys 构建标签意味着系统镜像使用公开可用的 AOSP 密钥签名，
 *    这是 Root ROM 的常见特征。
 * 2. **已知 Root 二进制文件**——常见位置存在 `su` 表示设备已被 Root。
 * 3. **Root 管理应用**——通过 `pm list packages` 查询 Magisk、SuperSU
 *    和其他 Root 管理包。
 * 4. **系统属性**——`ro.debuggable=1` 表示 ADB 以 root 权限运行，常见于
 *    工程构建和许多自定义 ROM。
 * 5. **`which su`**——在 shell 级别检查 PATH 中的 `su` 二进制文件。
 *
 * ## 局限性
 * 这些只是**启发式检测**，不是密码学证明。拥有内核级访问权限的高级攻击者
 * 可以隐藏此处列出的每一个指标。应将结果用作风险信号（例如禁用应用内支付），
 * 而非绝对的安全门禁。
 */
package com.example.sourcehub.security

import android.os.Build
import java.io.File

/**
 * 无状态 Root 检测工具。所有方法都是纯检查，可以从任意线程调用
 * （但 [checkRootManagementApps] 和 [checkSystemProperties] 通过 shell 调用
 * `Runtime.exec`，在生产代码中应在主线程之外调用）。
 */
object RootDetection {

    /**
     * 当检测到任何 Root 指标时返回 `true`。
     * 短路：一旦任一检查为阳性即返回。
     */
    fun isDeviceRooted(): Boolean {
        return checkBuildTags() ||
                checkRootBinaries() ||
                checkRootManagementApps() ||
                checkSystemProperties() ||
                checkSuCommand()
    }

    /**
     * 检查操作系统构建标签是否包含"test-keys"，这表示固件使用
     * Android 开源项目测试密钥签名——这是自定义/Root ROM 的强烈信号。
     */
    private fun checkBuildTags(): Boolean {
        val buildTags = Build.TAGS
        return buildTags != null && buildTags.contains("test-keys")
    }

    /**
     * 在文件系统中检查已知的 `su` 二进制文件位置。
     *
     * 这些路径涵盖了常见的 Root 方案：无系统 Root（`/sbin/su`）、
     * 系统分区 Root（`/system/bin/su`、`/system/xbin/su`）和
     * 应用安装的 Root（`/data/local/...`）。
     */
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

    /**
     * 查询包管理器以查找已知的 Root 管理应用。
     *
     * 使用 `pm list packages`（运行不需要 Root 权限）并扫描输出中的
     * Magisk、SuperSU 和其他常见 Root 管理器的包名。
     * 异常会被吞掉——如果 `pm` 不可用，检查返回 `false`。
     */
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

    /**
     * 检查 `ro.debuggable` 系统属性。
     *
     * 值为 `"1"` 表示设备上的所有 ADB 会话以 root 身份运行——
     * 这是工程构建和许多自定义 ROM 的默认值。
     */
    private fun checkSystemProperties(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("getprop ro.debuggable")
            val result = process.inputStream.bufferedReader().readText().trim()
            result == "1"
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 在 shell 中执行 `which su`。非空的 stdout 表示 `su` 二进制文件在 PATH 中——
     * 在 Root 二进制文件未被内核模块隐藏的设备上，这是一个可靠的指标。
     */
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
