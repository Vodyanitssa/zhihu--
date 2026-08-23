package com.zhihuminus.core.renderer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.zhihuminus.core.content.ContentNode
import com.zhihuminus.core.content.EmojiManager
import com.zhihuminus.core.content.InlineNode
import com.zhihuminus.feature.imageview.ImageViewManager
import com.zhihuminus.navigation.LocalNavigator
import com.zhihuminus.navigation.Video
import com.zhihuminus.platform.rememberExternalUrlOpener

/**
 * 图片查看管理器的 CompositionLocal。
 *
 * Screen 层通过 [CompositionLocalProvider] 提供 [ImageViewManager] 实例，
 * Renderer 层的 Image composable 在点击时直接调用 [ImageViewManager.show]，
 * 无需通过回调层层传递。
 */
val LocalImageViewManager = staticCompositionLocalOf<ImageViewManager?> { null }

/*
 * 渲染段落、表情包
 */
@Composable
fun InlineNodes(
    nodes: List<InlineNode>,
) {
    val inlineContent = EmojiManager.inlineContent
    val text = buildAnnotatedString {
        nodes.forEach { node ->
            appendInlineNode(node)
        }
    }
    Text(
        text = text,
        inlineContent = inlineContent,
    )
}

@Composable
fun EmojiItem(
    name: String,
    resource: String,
    modifier: Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = "file:///android_asset/emojis/images/$resource",
            contentDescription = name,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private fun AnnotatedString.Builder.appendInlineNode(
    node: InlineNode,
) {
    when (node) {
        is InlineNode.Text -> {
            append(node.text)
        }

        is InlineNode.Emoji -> {
            appendInlineContent(node.name)
        }

        is InlineNode.Bold -> {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                node.children.forEach { child ->
                    appendInlineNode(child)
                }
            }
        }

        is InlineNode.Italic -> {
            withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                node.children.forEach { child ->
                    appendInlineNode(child)
                }
            }
        }
    }
}

@Composable
fun ContentNodes(
    nodes: List<ContentNode>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        nodes.forEach { node ->
            ContentNode(node)
        }
    }
}

@Composable
fun ContentNode(
    node: ContentNode,
) {
    when (node) {
        is ContentNode.Paragraph -> {
            if (node.content.isNotEmpty()) {
                InlineNodes(node.content)
            }
        }

        is ContentNode.Heading -> {
            Text(
                text = node.content,
                style = when (node.level) {
                    1 -> MaterialTheme.typography.headlineLarge
                    2 -> MaterialTheme.typography.headlineMedium
                    3 -> MaterialTheme.typography.headlineSmall
                    4 -> MaterialTheme.typography.titleLarge
                    5 -> MaterialTheme.typography.titleMedium
                    else -> MaterialTheme.typography.titleSmall
                },
                fontWeight = FontWeight.Bold,
            )
        }

        is ContentNode.Splitter -> HorizontalDivider()

        is ContentNode.Link -> {
            Link(node)
        }

        is ContentNode.Code -> {
            Code(node)
        }

        is ContentNode.Quote -> {
            Quote(node)
        }

        is ContentNode.Image -> {
            Image(node)
        }

        is ContentNode.Video -> {
            Video(node)
        }

        is ContentNode.Listing -> {
            Listing(node)
        }

        is ContentNode.Table -> {
            Table(node)
        }
    }
}

@Composable
private fun Link(node: ContentNode.Link) {
    if (node.isCard) {
        val openExternalUrl = rememberExternalUrlOpener()
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { openExternalUrl(node.url) },
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = node.content,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    } else {
        val openExternalUrl = rememberExternalUrlOpener()
        val annotatedString = buildAnnotatedString {
            withLink(
                LinkAnnotation.Url(
                    url = node.url,
                    styles = TextLinkStyles(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline,
                        ),
                    ),
                    linkInteractionListener = {
                        openExternalUrl(node.url)
                    },
                ),
            ) {
                append(node.content)
            }
        }
        Text(
            text = annotatedString,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun Code(node: ContentNode.Code) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (node.language != "text") {
            Text(
                text = node.language,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SelectionContainer {
            Text(
                text = node.content,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

@Composable
private fun Quote(node: ContentNode.Quote) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(vertical = 10.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(24.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.primary),
        )

        InlineNodes(node.content)
    }
}

@Composable
private fun Image(node: ContentNode.Image) {
    val imageViewerManager = LocalImageViewManager.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AsyncImage(
            model = node.url,
            contentDescription = node.caption,
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .clickable { imageViewerManager?.show(node.url) },
            contentScale = ContentScale.FillWidth,
        )

        node.caption?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Video(node: ContentNode.Video) {
    val navigator = LocalNavigator.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
            contentAlignment = Alignment.Center,
        ) {
            val coverUrl = node.coverUrl
            if (coverUrl != null) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = node.caption,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp),
                    )
                }
            }

            IconButton(
                onClick = { navigator.onNavigate(Video(node.videoId)) },
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)),
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp),
                )
            }
        }

        node.caption?.let { caption ->
            Text(
                text = caption,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Listing(node: ContentNode.Listing) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        node.items.forEachIndexed { index, item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = if (node.isSorted) "${index + 1}." else "•",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.alignByBaseline(),
                )

                Text(
                    text = item,
                    modifier = Modifier
                        .weight(1f)
                        .alignByBaseline(),
                )
            }
        }
    }
}

@Composable
private fun Table(node: ContentNode.Table) {
    val columnCount = node.rows.maxOfOrNull { it.content.size } ?: return
    val tableWidth = 120.dp * columnCount

    // rows 已经包含 header；这里只需按 isHeader 给单元格设置样式，
    // 否则 header 会被渲染两次。
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
    ) {
        Column(modifier = Modifier.width(tableWidth)) {
            node.rows.forEachIndexed { rowIndex, row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                ) {
                    repeat(columnCount) { columnIndex ->
                        val cell = row.content.getOrNull(columnIndex)
                        TableCell(
                            text = cell?.content.orEmpty(),
                            isHeader = cell?.isHeader == true || row == node.header,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        )
                    }
                }

                if (rowIndex < node.rows.lastIndex) {
                    HorizontalDivider(thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
private fun TableCell(
    text: String,
    isHeader: Boolean,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
            ).background(
                if (isHeader) {
                    MaterialTheme.colorScheme.surfaceVariant
                } else {
                    Color.Transparent
                },
            ).padding(horizontal = 10.dp, vertical = 8.dp),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
    )
}
