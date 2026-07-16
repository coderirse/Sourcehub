package com.example.sourcehub.presentation.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.sourcehub.domain.model.Banner
import kotlinx.coroutines.delay

/**
 * 水平分页的横幅轮播组件，支持自动轮播和页面圆点指示器。
 *
 * 每个横幅渲染为圆角矩形卡片，包含通过 Coil 加载的全幅封面图片，
 * 以及底部带有垂直渐变遮罩的标题覆盖层以保证可读性。
 * 当有两个或以上横幅时，轮播每 4 秒自动切换。
 *
 * 页面指示器（圆点）渲染在分页器下方。激活的圆点略大并使用主色；
 * 未激活的圆点使用
 * [MaterialTheme.colorScheme.onSurfaceVariant] 的低透明度变体。
 *
 * 如果 [banners] 为空，组件立即返回，不渲染任何内容。
 *
 * 用法：
 * ```
 * BannerCarousel(
 *     banners = homeViewModel.banners,
 *     onBannerClick = { banner -> ... }
 * )
 * ```
 *
 * @param banners       要展示的 [Banner] 项列表。空列表不产生任何界面。
 * @param onBannerClick 点击 [Banner] 时调用的回调。
 * @param modifier      应用于外部 [Column] 的可选 [Modifier]。
 */
@Composable
fun BannerCarousel(
    banners: List<Banner>,
    onBannerClick: (Banner) -> Unit,
    modifier: Modifier = Modifier
) {
    // 短路返回：横幅列表为空时不渲染任何内容
    if (banners.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { banners.size })

    // 自动轮播循环：仅在多个横幅时激活
    LaunchedEffect(banners.size) {
        if (banners.size > 1) {
            while (true) {
                delay(4000) // 自动轮播间隔 4 秒
                val nextPage = (pagerState.currentPage + 1) % banners.size
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    Column(modifier = modifier) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2.67f) // 横幅 ~24:9 宽屏宽高比
        ) { page ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onBannerClick(banners[page]) }
            ) {
                // 全幅横幅图片
                AsyncImage(
                    model = banners[page].imageUrl,
                    contentDescription = banners[page].title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // 标题覆盖层，带垂直渐变遮罩以保证可读性
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomStart)
                        .height(48.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f))
                            )
                        )
                ) {
                    Text(
                        text = banners[page].title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White, // 始终白色以与遮罩形成对比
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                    )
                }
            }
        }

        // 页面指示器圆点——仅在多个横幅时渲染
        if (banners.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(banners.size) { index ->
                    // 激活圆点：8.dp，主色。未激活圆点：6.dp，低透明度。
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (pagerState.currentPage == index) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (pagerState.currentPage == index)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                    )
                }
            }
        }
    }
}
