package com.github.zly2006.zhihu.renderer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.platform.rememberExternalUrlOpener
import com.github.zly2006.zhihu.platform.rememberVideoPlayerOpener

/*
 * 渲染段落、表情包
 */
@Composable
fun RenderInlineNodes(
    nodes: List<InlineNode>,
) {
    val inlineContent = EmojiManager.inlineContent
    val text = buildAnnotatedString {
        nodes.forEachIndexed { index, node ->
            when (node) {
                is InlineNode.Text -> {
                    append(node.text)
                }

                is InlineNode.Emoji -> {
                    appendInlineContent(node.name)
                }
            }
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
private fun RenderContentNode(node: ContentNode) {
    when (node) {
        is ContentNode.Paragraph -> {
            if (node.content.isNotEmpty()) {
                RenderInlineNodes(node.content)
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
            RenderLink(node)
        }

        is ContentNode.Code -> {
            RenderCode(node)
        }

        is ContentNode.Quote -> {
            RenderQuote(node)
        }

        is ContentNode.Image -> {
            RenderImage(node)
        }

        is ContentNode.Video -> {
            RenderVideo(node)
        }

        is ContentNode.Listing -> {
            RenderListing(node)
        }

        is ContentNode.Table -> {
            RenderTable(node)
        }
    }
}

@Composable
private fun RenderLink(node: ContentNode.Link) {
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
private fun RenderCode(node: ContentNode.Code) {
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
private fun RenderQuote(node: ContentNode.Quote) {
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

        RenderInlineNodes(node.content)
    }
}

@Composable
private fun RenderImage(node: ContentNode.Image) {
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
                .clip(MaterialTheme.shapes.medium),
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
private fun RenderVideo(node: ContentNode.Video) {
    val openVideoPlayer = rememberVideoPlayerOpener()

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
                onClick = { openVideoPlayer(node.url, 0L) },
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
private fun RenderListing(node: ContentNode.Listing) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        node.items.forEachIndexed { index, item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (node.isSorted) "${index + 1}." else "•",
                    style = MaterialTheme.typography.bodyLarge,
                )

                Text(
                    text = item,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun RenderTable(node: ContentNode.Table) {
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
                Row(modifier = Modifier.fillMaxWidth()) {
                    repeat(columnCount) { columnIndex ->
                        val cell = row.content.getOrNull(columnIndex)
                        RenderTableCell(
                            text = cell?.content.orEmpty(),
                            isHeader = cell?.isHeader == true || row == node.header,
                            modifier = Modifier.weight(1f),
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
private fun RenderTableCell(
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
