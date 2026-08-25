package com.zhihuminus.core.content.renderer

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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.zhihuminus.core.content.ContentNode
import com.zhihuminus.core.content.EmojiManager
import com.zhihuminus.core.content.InlineNode
import com.zhihuminus.core.platform.copyText
import com.zhihuminus.feature.imageview.ImageViewManager
import com.zhihuminus.navigation.LocalNavigator
import com.zhihuminus.navigation.Video
import com.zhihuminus.platform.rememberExternalUrlOpener
import kotlinx.coroutines.launch

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
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    val openExternalUrl = rememberExternalUrlOpener()
    val inlineContent = EmojiManager.inlineContent
    val linkStyle = TextLinkStyles(
        style = SpanStyle(
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
        ),
    )
    val codeStyle = MaterialTheme.colorScheme.surfaceVariant
    val text = buildAnnotatedString {
        nodes.forEach { node ->
            appendInlineNode(node, openExternalUrl, linkStyle, codeStyle)
        }
    }
    Text(
        text = text,
        modifier = modifier,
        style = style,
        color = color,
        maxLines = maxLines,
        overflow = overflow,
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
    uriHandler: (String) -> Unit,
    linkStyle: TextLinkStyles,
    codeStyle: Color,
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
                    appendInlineNode(child, uriHandler, linkStyle, codeStyle)
                }
            }
        }

        is InlineNode.Italic -> {
            withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                node.children.forEach { child ->
                    appendInlineNode(child, uriHandler, linkStyle, codeStyle)
                }
            }
        }

        is InlineNode.Code -> {
            withStyle(
                SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    background = codeStyle,
                ),
            ) {
                append(" ${node.text} ")
            }
        }

        is InlineNode.Link -> {
            val link =
                LinkAnnotation.Url(
                    url = node.url,
                    styles = linkStyle,
                ) {
                    val url = (it as LinkAnnotation.Url).url
                    uriHandler(url)
                }
            withLink(
                link,
            ) {
                append(node.name)
            }
        }

        is InlineNode.LineBreak -> {
            appendLine()
        }
    }
}

@Composable
fun RenderContentNodes(
    nodes: List<ContentNode>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        nodes.forEach { node ->
            RenderContentNode(node)
        }
    }
}

@Composable
fun RenderContentNode(
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
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(text = node.url, style = MaterialTheme.typography.labelSmall)
                }
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
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        // 顶栏：语言标签 + 复制按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = node.language,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            IconButton(
                onClick = {
                    coroutineScope.launch {
                        clipboard.copyText(node.content)
                    }
                },
                modifier = Modifier.size(28.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "复制代码",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        HorizontalDivider(thickness = 1.dp)

        // 代码内容
        SelectionContainer {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(12.dp),
            ) {
                Text(
                    text = node.content,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    softWrap = false,
                )
            }
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AsyncImage(
            model = node.url,
            contentDescription = node.caption,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { imageViewerManager?.show(node.url) },
            contentScale = ContentScale.FillWidth,
        )
    }
    node.caption?.let {
        Text(
            text = it,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
