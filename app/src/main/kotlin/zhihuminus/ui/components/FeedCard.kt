/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.zhihuminus.ui.components

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.zhihuminus.core.content.AstParser
import com.zhihuminus.core.content.renderer.InlineNodes
import com.zhihuminus.core.settings.LocalAppSettings
import com.zhihuminus.core.util.formatDateTime
import com.zhihuminus.data.FeedDisplayItem
import org.jsoup.Jsoup

/**
 * 信息流卡片的纯 UI 渲染配置。
 *
 * 封装字体与行高缩放比例，解耦底层持久化存储，方便 Compose 树统一向下传递及 Preview/测试。
 */
@Immutable
data class FeedCardConfig(
    val fontSizePercent: Int = 100,
    val lineHeightPercent: Int = 160,
)

/**
 * 信息流卡片配置的 CompositionLocal，默认为常规标准配置。
 */
val LocalFeedCardConfig = staticCompositionLocalOf { FeedCardConfig() }

/**
 * 从全局设置中响应式读取并计算 [FeedCardConfig]，供上层容器注入给 [LocalFeedCardConfig]。
 */
@Composable
fun rememberFeedCardConfig(): FeedCardConfig {
    val reading = LocalAppSettings.current.reading
    return remember(reading.fontSizePercent, reading.lineHeightPercent) {
        FeedCardConfig(
            fontSizePercent = reading.fontSizePercent,
            lineHeightPercent = reading.lineHeightPercent,
        )
    }
}

/**
 * 信息流卡片的 Material 3 纯 UI 实现。
 *
 * 卡片自上而下展示来源标签、标题、作者（头像、名称、徽章）、摘要、缩略图和统计数据，始终使用 Duo3 排版。
 * 卡片作为无副作用的纯展示组件，不直接依赖导航器。点击事件通过 [onClick]/[onAuthorClick] 上报，
 * 显示与排版样式通过 [LocalFeedCardConfig] 或显式参数传入；页面可以通过 [menuItems] 声明业务长按菜单。
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun FeedCard(
    item: FeedDisplayItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onAuthorClick: (() -> Unit)? = null,
    maxHeight: Dp = 240.dp,
    thumbnailUrl: String? = null,
    horizontalPadding: Dp = 16.dp,
    showSourceLabel: Boolean = false,
    fontSizePercent: Int = LocalFeedCardConfig.current.fontSizePercent,
    lineHeightPercent: Int = LocalFeedCardConfig.current.lineHeightPercent,
    menuItems: @Composable ColumnScope.(dismissMenu: () -> Unit) -> Unit = { _ -> },
) {
    var showMenu by remember { mutableStateOf(false) }
    val effectiveThumbnailUrl = thumbnailUrl ?: item.thumbnailUrl
    val pinImages = item.pinImages
    val hasPinImages = pinImages.isNotEmpty()

    Box(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (hasPinImages) Modifier else Modifier.heightIn(max = maxHeight)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick ?: { showMenu = true },
                    ).padding(horizontal = horizontalPadding, vertical = 12.dp),
            ) {
                FeedCardContent(
                    item = item,
                    thumbnailUrl = effectiveThumbnailUrl,
                    pinImages = pinImages,
                    showSourceLabel = showSourceLabel,
                    fontSizePercent = fontSizePercent,
                    lineHeightPercent = lineHeightPercent,
                    onAuthorClick = onAuthorClick,
                )
            }
            HorizontalDivider(thickness = 0.3.dp)
        }
        FeedCardMenu(
            showMenu = showMenu,
            onShowMenuChange = { showMenu = it },
            menuItems = menuItems,
        )
    }
}

/**
 * 信息流卡片的长按菜单。
 *
 * 卡片只负责菜单的展开、收起和通用设置项；页面业务动作由 [menuItems] 直接提供。
 */
@Composable
private fun FeedCardMenu(
    showMenu: Boolean,
    onShowMenuChange: (Boolean) -> Unit,
    menuItems: @Composable ColumnScope.(dismissMenu: () -> Unit) -> Unit,
) {
    DropdownMenu(
        expanded = showMenu,
        onDismissRequest = { onShowMenuChange(false) },
    ) {
        menuItems { onShowMenuChange(false) }
    }
}

/**
 * 卡片正文内容。
 *
 * 这里决定来源标签、作者行、标题、摘要、缩略图和统计数据在卡片内的排列方式。
 */
@Composable
private fun FeedCardContent(
    item: FeedDisplayItem,
    thumbnailUrl: String?,
    pinImages: List<String>,
    showSourceLabel: Boolean,
    fontSizePercent: Int,
    lineHeightPercent: Int,
    onAuthorClick: (() -> Unit)?,
) {
    val sourceLabel = item.sourceLabel
    val typeLabel = item.contentTypeLabel
    // ── 卡片排版：来源标签 → 作者行 → 标题 → 摘要 → 图片 → 统计行 ─────────────────────
    if (showSourceLabel) {
        FeedCardSourceLabel(sourceLabel)
    }
    if (!item.title.isEmpty()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = searchHighlightedText(item.title),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                color = MaterialTheme.colorScheme.onSurface,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
    }
    if (item.avatarSrc != null && item.authorName != null) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(bottom = 8.dp)
                .then(
                    if (onAuthorClick != null) {
                        Modifier.clickable(onClick = onAuthorClick)
                    } else {
                        Modifier
                    },
                ),
        ) {
            AsyncImage(
                model = item.avatarSrc,
                contentDescription = "Avatar",
                modifier = Modifier
                    .clip(CircleShape)
                    .size(24.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = item.authorName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val authorBadge = item.authorBadge
            if (authorBadge?.isUsefulInList == true) {
                Spacer(Modifier.width(4.dp))
                AuthorBadge(authorBadge, compact = true)
            }
        }
    }

    val summaryNodes = remember(item.summary) {
        item.summary
            ?.takeIf(String::isNotBlank)
            ?.let { html -> Jsoup.parseBodyFragment(html).body().childNodes() }
            ?.flatMap { AstParser.parseInline(it) }
            .orEmpty()
    }
    Column {
        Row {
            InlineNodes(
                nodes = summaryNodes,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp * fontSizePercent / 100,
                    lineHeight = 14.sp * fontSizePercent / 100 * lineHeightPercent / 100,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (!thumbnailUrl.isNullOrEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = "Thumbnail",
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .sizeIn(maxHeight = 80.dp, maxWidth = 128.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.FillHeight,
                )
            }
        }
        PinFeedImages(
            images = pinImages,
            modifier = Modifier.padding(top = 8.dp),
        )
        val statsText = typeLabel
            ?.takeIf { item.details.startsWith("$it · ") }
            ?.let { item.details.removePrefix("$it · ") }
            ?: item.details
        val publishTimeText = item.publishTimeSeconds
            ?.let(::formatDateTime)
        if (statsText.isNotEmpty() || publishTimeText != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (statsText.isNotEmpty()) {
                    Text(
                        text = statsText,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
                publishTimeText?.let { time ->
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = time,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

internal enum class PinFeedImageLayout {
    SINGLE,
    MULTI_ROW,
    NINE_GRID,
}

internal fun pinFeedImageLayout(imageCount: Int): PinFeedImageLayout? = when (imageCount) {
    0 -> null
    1 -> PinFeedImageLayout.SINGLE
    in 2..4 -> PinFeedImageLayout.MULTI_ROW
    else -> PinFeedImageLayout.NINE_GRID
}

@Composable
private fun PinFeedImages(
    images: List<String>,
    modifier: Modifier = Modifier,
) {
    when (pinFeedImageLayout(images.size)) {
        null -> return
        PinFeedImageLayout.SINGLE -> {
            val image = images.single()
            AsyncImage(
                model = image,
                contentDescription = "想法图片 1/1",
                modifier = modifier
                    .fillMaxWidth(1f / 3f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
            )
        }

        PinFeedImageLayout.MULTI_ROW -> {
            Row(
                modifier = modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                images.forEachIndexed { index, image ->
                    PinFeedImage(
                        imageUrl = image,
                        index = index,
                        totalCount = images.size,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                    )
                }
                repeat((3 - images.size).coerceAtLeast(0)) {
                    Spacer(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                    )
                }
            }
        }

        PinFeedImageLayout.NINE_GRID -> {
            val visibleImages = images.take(9)
            Column(
                modifier = modifier
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                visibleImages.chunked(3).forEachIndexed { rowIndex, rowImages ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        rowImages.forEachIndexed { columnIndex, image ->
                            val index = rowIndex * 3 + columnIndex
                            PinFeedImage(
                                imageUrl = image,
                                index = index,
                                totalCount = images.size,
                                remainingCount = (images.size - 9).takeIf { index == 8 && it > 0 },
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f),
                            )
                        }
                        repeat(3 - rowImages.size) {
                            Spacer(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PinFeedImage(
    imageUrl: String,
    index: Int,
    totalCount: Int,
    remainingCount: Int? = null,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp)),
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = "想法图片 ${index + 1}/$totalCount",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        if (remainingCount != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "+$remainingCount",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }
    }
}

@Composable
private fun FeedCardSourceLabel(sourceLabel: String?) {
    val label = sourceLabel?.takeIf { it.isNotBlank() } ?: return
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(bottom = 6.dp),
    )
}
