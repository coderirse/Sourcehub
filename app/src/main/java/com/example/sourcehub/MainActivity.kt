/**
 * SourceHub Android 应用的单 Activity 宿主。
 *
 * 此 [ComponentActivity] 是应用中唯一的 Activity（单 Activity 架构）。
 * 所有屏幕作为 Compose 目的地渲染在 [NavHost] 内；Activity 本身
 * 仅设置边缘到边缘渲染、Compose 主题和根 [NavGraph]。
 *
 * 空的 `// 防截屏……` 注释是为显示敏感内容（如支付）的屏幕
 * 应用 [android.view.WindowManager.LayoutParams.FLAG_SECURE] 逻辑的占位符。
 */
package com.example.sourcehub

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.sourcehub.navigation.NavGraph
import com.example.sourcehub.ui.theme.SourcehubTheme

/**
 * 通过 [NavGraph] 承载所有 Compose UI 的单一 Activity。
 *
 * 启用边缘到边缘渲染，使应用绘制到系统栏（状态栏、
 * 导航栏）之后，呈现现代全屏效果。Compose [SourcehubTheme] 在顶层应用，
 * 因此每个屏幕都会继承应用的 Material 3 配色方案和排版。
 */
class MainActivity : ComponentActivity() {

    /**
     * 配置边缘到边缘渲染并设置 Compose 内容树。
     *
     * [enableEdgeToEdge] 必须在 [setContent] 之前调用，以便 Compose `WindowInsets`
     * API 能正确计算系统栏内边距。
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 在安全上下文中防截屏（通过 FLAG_SECURE 逐屏处理）

        setContent {
            SourcehubTheme {
                NavGraph()
            }
        }
    }
}
