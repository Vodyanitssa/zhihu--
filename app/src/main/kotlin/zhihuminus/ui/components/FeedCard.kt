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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.zhihuminus.core.content.AstParser
import com.zhihuminus.core.content.renderer.InlineNodes
import com.zhihuminus.core.util.formatDateTime
import com.zhihuminus.data.DataHolder
import com.zhihuminus.data.Feed
import com.zhihuminus.data.FeedDisplayItem
import com.zhihuminus.data.navDestination
import com.zhihuminus.data.officialBadge
import com.zhihuminus.data.sourceLabel
import com.zhihuminus.data.target
import com.zhihuminus.navigation.LocalNavigator
import com.zhihuminus.navigation.NavDestination
import com.zhihuminus.navigation.withReadingQueueSource
import com.zhihuminus.platform.UserMessageDuration
import com.zhihuminus.platform.rememberSettingsStore
import com.zhihuminus.platform.rememberUserMessageSink
import com.zhihuminus.ui.subscreens.PREF_FONT_SIZE
import com.zhihuminus.ui.subscreens.PREF_LINE_HEIGHT
import com.zhihuminus.util.parseEmphasizedHtmlTextWithTheme
import org.jsoup.Jsoup

/**
 * 信息流卡片的 Material 3 实现。
 *
 * 卡片自上而下展示来源标签、标题、作者（头像、名称、徽章）、摘要、缩略图和统计数据，始终使用 Duo3 排版。
 * 默认点击会解析 [FeedDisplayItem] 的导航目标并进入详情页；页面可以通过 [menuItems] 直接声明自己的业务菜单项，长按卡片弹出。
 *
 * 修改这个组件时要同步复核 `showFeedThumbnail` 设置对各信息流入口的影响。
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun FeedCard(
    item: FeedDisplayItem,
    modifier: Modifier = Modifier,
    readingQueueSourceId: String? = null,
    maxHeight: Dp = 240.dp,
    thumbnailUrl: String? = null,
    horizontalPadding: Dp = 16.dp,
    menuItems: @Composable ColumnScope.(dismissMenu: () -> Unit) -> Unit = { _ -> },
    showSourceLabel: Boolean = false,
    /**
     * 默认点击行为：优先跳转到信息流条目的详情页；如果只能识别为外链则打开外链，否则提示暂不支持。
     */
    onClick: ((item: FeedDisplayItem, destination: NavDestination?) -> Unit)? = null,
) {
    val navigator = LocalNavigator.current
    val uriHandler = LocalUriHandler.current
    val userMessages = rememberUserMessageSink()
    val settings = rememberSettingsStore()
    var showMenu by remember { mutableStateOf(false) }
    val showFeedThumbnail = remember {
        settings.getBoolean("showFeedThumbnail", true)
    }
    val pinImages = (item.feed?.target as? Feed.PinTarget)
        ?.content
        ?.filterIsInstance<DataHolder.Pin.ContentImage>()
        .orEmpty()
    val showPinImages = showFeedThumbnail && pinImages.isNotEmpty()
    val performClick: (FeedDisplayItem) -> Unit = { clickedItem ->
        val destination = clickedItem.navDestination?.withReadingQueueSource(readingQueueSourceId)
        if (onClick != null) {
            onClick(clickedItem, destination)
        } else {
            destination?.let(navigator.onNavigate) ?: run {
                if (clickedItem.content?.startsWith("http") == true) {
                    uriHandler.openUri(clickedItem.content)
                } else {
                    userMessages.showMessage("暂不支持打开该内容", UserMessageDuration.Short)
                }
            }
        }
    }
    Box(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (showPinImages) Modifier else Modifier.heightIn(max = maxHeight)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(onClick = { performClick(item) }, onLongClick = { showMenu = true })
                    .padding(horizontal = horizontalPadding, vertical = 12.dp),
            ) {
                FeedCardContent(
                    item = item,
                    showFeedThumbnail = showFeedThumbnail,
                    thumbnailUrl = thumbnailUrl,
                    pinImages = pinImages,
                    showSourceLabel = showSourceLabel,
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
    showFeedThumbnail: Boolean,
    thumbnailUrl: String?,
    pinImages: List<DataHolder.Pin.ContentImage>,
    showSourceLabel: Boolean,
) {
    val settings = rememberSettingsStore()
    val fontSizePercent = remember { settings.getInt(PREF_FONT_SIZE, 100) }
    val lineHeightPercent = remember { settings.getInt(PREF_LINE_HEIGHT, 160) }
    val visiblePinImages = pinImages.takeIf { showFeedThumbnail }.orEmpty()
    val sourceLabel = item.feed?.sourceLabel
    val typeLabel = item.contentTypeLabel
    // ── 卡片排版：来源标签 → 作者行 → 标题 → 摘要 → 图片 → 统计行 ─────────────────────
    if (showSourceLabel) {
        FeedCardSourceLabel(sourceLabel)
    }
    if (!item.title.isEmpty()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = parseEmphasizedHtmlTextWithTheme(item.title),
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
                .clickable {},
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
            val authorBadge = item.authorBadgeV2.officialBadge()
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
            if (!thumbnailUrl.isNullOrEmpty() && showFeedThumbnail) {
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
            images = visiblePinImages,
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
    images: List<DataHolder.Pin.ContentImage>,
    modifier: Modifier = Modifier,
) {
    when (pinFeedImageLayout(images.size)) {
        null -> return
        PinFeedImageLayout.SINGLE -> {
            val image = images.single()
            AsyncImage(
                model = image.feedThumbnailUrl,
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
                        image = image,
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
                                image = image,
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
    image: DataHolder.Pin.ContentImage,
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
            model = image.feedThumbnailUrl,
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

internal val DataHolder.Pin.ContentImage.feedThumbnailUrl: String
    get() = thumbnail.ifBlank { url }

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
