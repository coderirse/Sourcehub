/**
 * 商品预览页面，在水平分页器中渲染最多 [PREVIEW_MAX_PAGES] 个生成的位图页面。
 *
 * 由于当前版本未使用真实的 PDF 渲染，预览页面通过 Android [Canvas] 绘制，
 * 包含占位内容（标题、描述摘要、目录等）。每一页都叠加有半透明的
 * "PREVIEW - SourceHub" 水印。
 *
 * 该页面还会启用 [WindowManager.LayoutParams.FLAG_SECURE]，
 * 以在预览可见时阻止截屏。
 */
package com.example.sourcehub.presentation.product.preview

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sourcehub.presentation.common.components.ErrorView
import com.example.sourcehub.presentation.common.components.LoadingIndicator

/** 预览分页器中展示的最大页数。 */
private const val PREVIEW_MAX_PAGES = 3
/** 虚拟页面宽度（像素，缩放前的完整尺寸）。 */
private const val PAGE_WIDTH = 1240
/** 虚拟页面高度（像素，缩放前的完整尺寸）。 */
private const val PAGE_HEIGHT = 1754

/**
 * 使用 [HorizontalPager] 和 Canvas 生成的占位页面展示预览。
 *
 * 进入时启用安全窗口标志（禁用截屏），退出时通过 [DisposableEffect] 恢复。
 * 系统返回手势由 [BackHandler] 拦截，确保用户始终返回到详情页面。
 *
 * @param productId 要显示预览的商品 ID。
 * @param onNavigateBack 返回箭头或系统返回手势触发时调用。
 * @param viewModel 加载预览元数据的 [ProductPreviewViewModel]。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductPreviewScreen(
    productId: String,
    onNavigateBack: () -> Unit,
    viewModel: ProductPreviewViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val view = LocalView.current
    val context = LocalContext.current

    // 启用 FLAG_SECURE 以在预览可见时阻止截屏。
    DisposableEffect(Unit) {
        view.setWindowSecureFlag(true)
        onDispose { view.setWindowSecureFlag(false) }
    }

    LaunchedEffect(productId) { viewModel.loadPreview(productId) }

    // 拦截系统返回以回到详情页，而非退出页面栈。
    BackHandler(enabled = true) { onNavigateBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("预览 - 仅展示前${PREVIEW_MAX_PAGES}页") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> LoadingIndicator(Modifier.padding(padding))
            uiState.error != null -> ErrorView(uiState.error!!, { viewModel.loadPreview(productId) }, Modifier.padding(padding))
            else -> {
                // 将页数限制在 1 到 PREVIEW_MAX_PAGES 之间，
                // 即使商品为 0 页，分页器也始终至少有一页。
                val pageCount = minOf(PREVIEW_MAX_PAGES, maxOf(1, uiState.pageCount))
                val pagerState = rememberPagerState(pageCount = { pageCount })

                Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                    // 页面指示器，显示当前页 / 总页数。
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${pagerState.currentPage + 1} / $pageCount",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // 分页器 — 每一页是按需生成并通过 remember 缓存的位图。
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    ) { page ->
                        // 仅在标题、描述或页面索引变化时重新生成位图。
                        val bitmap = remember(uiState.productTitle, uiState.productDescription, page) {
                            generatePreviewPage(
                                uiState.productTitle,
                                uiState.productDescription,
                                page
                            )
                        }
                        Card(
                            modifier = Modifier.fillMaxSize().padding(4.dp),
                            shape = RoundedCornerShape(4.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                bitmap?.let { bmp ->
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "预览第${page + 1}页",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                // 叠加在每张预览页面上的半透明水印。
                                Text(
                                    text = "PREVIEW - SourceHub",
                                    color = androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.12f),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .padding(32.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Bottom hint explaining preview limitations.
                    Text(
                        text = "此为预览版本，仅展示部分内容。左右滑动翻页。购买后可下载完整文件。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

/**
 * 使用 Android [Canvas] API 生成占位预览页面的位图。
 *
 * 在生产构建中，这将替换为真实的 [android.graphics.pdf.PdfRenderer] 渲染。
 * 当前实现绘制：
 * - 第 0 页：商品标题和描述的前 500 个字符。
 * - 第 1 页：硬编码的目录及"购买以查看完整内容"提示。
 * - 第 2 页：扩展的描述摘要（对于短描述会重复填充）。
 *
 * 所有页面均包含页眉（"SourceHub 预览"）、带页码的页脚以及页眉下方的水平分隔线。
 *
 * @param title 显示在第一页上的商品标题。
 * @param description 跨页面摘录的商品描述。
 * @param pageIndex 从 0 开始的页面索引（0-2）。
 * @return 渲染页面的 [Bitmap]，如果绘制失败则返回 null。
 */
private fun generatePreviewPage(title: String, description: String, pageIndex: Int): Bitmap? {
    return try {
        // 缩放因子将虚拟页面尺寸减半以节省内存。
        val scale = 0.5f
        val w = (PAGE_WIDTH * scale).toInt()
        val h = (PAGE_HEIGHT * scale).toInt()
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 白色背景
        canvas.drawColor(android.graphics.Color.WHITE)

        // 正文文字画笔 — 用于描述和通用内容。
        val textPaint = Paint().apply {
            color = android.graphics.Color.DKGRAY
            textSize = 32f
            isAntiAlias = true
        }
        // 标题画笔 — 加粗、较大，用于商品标题。
        val titlePaint = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 48f
            isAntiAlias = true
            isFakeBoldText = true
        }
        // 页眉/页脚画笔 — 较小、灰色，用于品牌标识和页码。
        val headerPaint = Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = 24f
            isAntiAlias = true
        }
        // 分隔线画笔 — 页眉下方的浅灰色分隔线。
        val linePaint = Paint().apply {
            color = android.graphics.Color.LTGRAY
            strokeWidth = 2f
        }

        val padding = 60f
        // 从顶部内边距下方开始绘制，为页眉文字留出额外空间。
        var y = padding + 60f

        // 页眉 — 品牌标签。
        canvas.drawText("SourceHub 预览", padding, y, headerPaint)
        y += 50f

        // 页眉下方的水平分隔线。
        canvas.drawLine(padding, y, w - padding, y, linePaint)
        y += 50f

        // 标题 — 自动换行，使长标题不会超出页面宽度。
        val titleLines = wrapText(title, titlePaint, w - padding * 2)
        titleLines.forEach { line ->
            canvas.drawText(line, padding, y, titlePaint)
            y += 60f
        }
        y += 30f

        // 页面特定内容 — 每个页面索引绘制不同的布局。
        when (pageIndex) {
            0 -> {
                // 第 0 页：商品简介（描述的前 500 个字符）。
                canvas.drawText("商品简介", padding, y, textPaint)
                y += 45f
                val descLines = wrapText(description.take(500), textPaint, w - padding * 2)
                // 限制为 18 行以避免超出页面范围。
                descLines.take(18).forEach { line ->
                    canvas.drawText(line, padding, y, textPaint)
                    y += 38f
                }
            }
            1 -> {
                // 第 1 页：硬编码的目录作为文档结构的预览。
                canvas.drawText("内容概览", padding, y, textPaint)
                y += 45f
                val sections = listOf(
                    "1. 概述与适用场景",
                    "2. 核心功能特性",
                    "3. 使用方法与步骤",
                    "4. 常见问题解答",
                    "5. 技术支持与更新"
                )
                sections.forEach { section ->
                    canvas.drawText(section, padding + 20f, y, textPaint)
                    y += 45f
                }
                y += 30f
                // 灰色页脚提示，鼓励用户购买。
                canvas.drawText("（完整内容请购买后查看）", padding + 20f, y, textPaint.apply { color = android.graphics.Color.GRAY })
            }
            2 -> {
                // 第 2 页：扩展的描述摘要。
                canvas.drawText("部分内容节选", padding, y, textPaint)
                y += 45f
                // 对于很短的描述，重复文本以填满页面；否则最多使用 800 个字符。
                val sampleText = description.ifEmpty { "此为示例预览页面。购买后可获取完整${title}的全部内容。" }
                val sampleLines = wrapText(sampleText.repeat(if (sampleText.length < 100) 3 else 1).take(800), textPaint, w - padding * 2)
                sampleLines.take(20).forEach { line ->
                    canvas.drawText(line, padding, y, textPaint)
                    y += 38f
                }
                y += 40f
                // 摘要底部的省略号提示。
                canvas.drawText("... 更多内容请购买后查看 ...", padding, y, textPaint.apply { color = android.graphics.Color.GRAY; textSize = 28f })
            }
        }

        // 页脚 — 页码，固定在页面底部附近。
        y = h - padding - 30f
        canvas.drawText("SourceHub © ${pageIndex + 1}/$PREVIEW_MAX_PAGES", padding, y, headerPaint)

        bitmap
    } catch (e: Exception) {
        // 绘制失败时返回 null，界面将优雅地显示空白页面。
        null
    }
}

/**
 * 将 [text] 自动换行为适合 [maxWidth] 的行，使用 [paint] 进行渲染。
 *
 * 按空格字符分割，逐词累积到一行中，直到该行的测量宽度超过 [maxWidth]，
 * 此时将该行输出并开始新的一行。
 *
 * @param text 要换行的输入文本。
 * @param paint 用于测量文本边界的 [Paint]。
 * @param maxWidth 每行的最大宽度（像素）。
 * @return 换行后的行列表，每行保证不超过 [maxWidth]。
 */
private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
    val lines = mutableListOf<String>()
    val words = text.split(" ")
    var currentLine = StringBuilder()
    val rect = Rect()

    for (word in words) {
        val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
        paint.getTextBounds(testLine, 0, testLine.length, rect)
        if (rect.width() > maxWidth && currentLine.isNotEmpty()) {
            // 当前行会超出 — 输出它并开始新的一行。
            lines.add(currentLine.toString())
            currentLine = StringBuilder(word)
        } else {
            if (currentLine.isNotEmpty()) currentLine.append(" ")
            currentLine.append(word)
        }
    }
    // 输出最后一行中剩余的文字。
    if (currentLine.isNotEmpty()) lines.add(currentLine.toString())
    return lines
}

/**
 * 在 Activity 窗口上切换 [WindowManager.LayoutParams.FLAG_SECURE] 标志。
 *
 * 启用后，系统将阻止截屏、屏幕录制以及内容出现在最近应用缩略图中。
 * 用于保护预览内容不被未经授权的捕获。
 *
 * @param enable 是否添加（true）或清除（false）安全标志。
 */
private fun android.view.View.setWindowSecureFlag(enable: Boolean) {
    if (enable) {
        (context as? android.app.Activity)?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    } else {
        (context as? android.app.Activity)?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
}
