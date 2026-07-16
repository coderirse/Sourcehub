package com.example.sourcehub.presentation.common.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.sourcehub.domain.model.Product

/**
 * 用于网格和列表布局的商品卡片组件。
 *
 * 渲染一个 [Card]，包含通过 Coil 异步加载的封面图片、
 * 商品标题（最多2行）、以粗体主色显示的当前价格、
 * 销售数量徽章，以及有折扣时以降低透明度显示的划线原价。
 *
 * 图片使用 0.7 竖版宽高比，并通过匹配的 [RoundedCornerShape]
 * 裁剪与卡片顶部圆角对齐。
 *
 * 用法：
 * ```
 * ProductCard(
 *     product = productItem,
 *     onClick = { navController.navigate("detail/${productItem.id}") }
 * )
 * ```
 *
 * @param product  持有待展示数据的领域模型。
 * @param onClick  点击整个卡片时调用的回调。
 * @param modifier 应用于 [Card] 的可选 [Modifier]。
 */
@Composable
fun ProductCard(
    product: Product,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Coil AsyncImage 加载封面 URL，自动缓存
            AsyncImage(
                model = product.coverUrl,
                contentDescription = product.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.7f) // 商品封面的竖版宽高比
                    // 裁剪与卡片顶部圆角形状匹配
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = product.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                // 价格行：左侧当前价格，右侧销售数量
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "¥${String.format("%.2f", product.price)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${product.salesCount}人购买",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // 仅当有折扣时显示原价（originalPrice > price）
                if (product.originalPrice > product.price) {
                    Text(
                        text = "¥${String.format("%.2f", product.originalPrice)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
