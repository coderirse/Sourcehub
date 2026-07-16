/**
 * Android 模拟器/虚拟环境检测。
 *
 * 此对象检查多个系统属性和文件系统痕迹，这些是 Android 模拟器（AVD）、
 * Genymotion、基于 VirtualBox 的镜像以及其他虚拟化 Android 环境的特征。
 *
 * ## 检测信号
 * - **Build.FINGERPRINT**——原版模拟器镜像在指纹中包含 "generic"、"vbox" 或
 *   "sdk_gphone"。
 * - **Build.MODEL**——模拟器型号通常是 "google_sdk"、"sdk"、"sdk_x86"
 *   或 "Android SDK built for x86"。
 * - **Build.MANUFACTURER**——"Genymotion"、"virtual" 或 "unknown" 是常见的。
 * - **Build.HARDWARE**——"goldfish" 和 "ranchu" 是官方 Android 模拟器使用的 QEMU
 *   虚拟板卡；"vbox86" 表示 VirtualBox。
 * - **模拟器专用文件**——`/dev/socket/qemud`、`/dev/qemu_pipe` 等仅存在于
 *   模拟设备上。
 *
 * ## 局限性
 * 这些是启发式方法。有决心的用户可以编辑 `build.prop` 或使用伪装真实设备特征的
 * 自定义模拟器镜像。该检查应用作更广泛风险评估决策的一个输入，
 * 而非独立的安全门禁。
 */
package com.example.sourcehub.security

import android.os.Build
import java.io.File

/**
 * 无状态模拟器检测工具。所有检查均为纯读取系统属性
 * 或文件是否存在——不涉及网络调用，可安全地从主线程调用。
 */
object EmulatorDetection {

    /**
     * 当检测到任何模拟器指标时返回 `true`。
     * 在第一个阳性结果时短路返回。
     */
    fun isEmulator(): Boolean {
        return checkFingerprint() ||
                checkModel() ||
                checkManufacturer() ||
                checkHardware() ||
                checkEmulatorFiles()
    }

    /**
     * 检查 [Build.FINGERPRINT] 中已知的模拟器子字符串。
     *
     * Android 模拟器的指纹通常以 "generic" 开头或包含 "sdk_gphone"。
     * 基于 VirtualBox 的镜像通常包含 "vbox"。
     */
    private fun checkFingerprint(): Boolean {
        val fingerprint = Build.FINGERPRINT
        return fingerprint.startsWith("generic") ||
                fingerprint.startsWith("unknown") ||
                fingerprint.contains("generic") ||
                fingerprint.contains("vbox") ||
                fingerprint.contains("sdk_gphone")
    }

    /**
     * 检查 [Build.MODEL]（大小写不敏感）中模拟器特定的型号名称。
     *
     * 涵盖官方模拟器（"google_sdk"、"Android SDK built for x86"）、
     * 旧版 "sdk"/"sdk_x86" 型号，以及通用的 "emulator"/"virtual" 字符串。
     */
    private fun checkModel(): Boolean {
        val model = Build.MODEL.lowercase()
        return model.contains("google_sdk") ||
                model.contains("emulator") ||
                model.contains("android sdk built for x86") ||
                model.contains("virtual") ||
                model == "sdk" ||
                model == "sdk_x86"
    }

    /**
     * 检查 [Build.MANUFACTURER] 中虚拟环境厂商的名称。
     *
     * "Genymotion" 是 Genymotion 模拟器；"virtual" 和 "unknown"/"android"
     * 是模拟器镜像中常见的回退值。
     */
    private fun checkManufacturer(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return manufacturer.contains("genymotion") ||
                manufacturer.contains("virtual") ||
                manufacturer.contains("android")
    }

    /**
     * 检查 [Build.HARDWARE] 中 QEMU/VirtualBox 的板卡名称。
     *
     * "goldfish" 和 "ranchu" 是官方 Android 模拟器在不同 API 级别中
     * 使用的两个 QEMU 虚拟板卡。"vbox86" 表示 VirtualBox。
     */
    private fun checkHardware(): Boolean {
        val hardware = Build.HARDWARE.lowercase()
        return hardware.contains("goldfish") ||
                hardware.contains("ranchu") ||
                hardware.contains("vbox86") ||
                hardware == "sdk"
    }

    /**
     * 检查 Android 模拟器或基于 QEMU 的虚拟环境特有的文件和设备节点是否存在。
     *
     * 这些路径是模拟器硬件抽象层（HAL）的一部分，
     * 在物理设备上不存在。
     */
    private fun checkEmulatorFiles(): Boolean {
        val paths = arrayOf(
            "/dev/socket/qemud",       // QEMU 守护进程套接字
            "/dev/qemu_pipe",          // QEMU 管道设备
            "/proc/tty/drivers",       // 在模拟器上通常包含 "goldfish"
            "/system/bin/qemu-props",  // QEMU 属性模拟
            "/init.goldfish.rc"        // Goldfish 初始化脚本
        )
        return paths.any { File(it).exists() }
    }
}
