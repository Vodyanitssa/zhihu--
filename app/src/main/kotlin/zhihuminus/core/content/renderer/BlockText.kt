package com.zhihuminus.core.content.renderer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import com.zhihuminus.core.content.ContentNode
import com.zhihuminus.core.platform.copyText
import com.zhihuminus.navigation.link.rememberInAppLinkOpener
import com.zhihuminus.theme.CodeFontFamily
import kotlinx.coroutines.launch

@Composable
internal fun Link(
    node: ContentNode.Link,
    onClick: ((String) -> Unit)? = null,
) {
    val defaultOpenLink = rememberInAppLinkOpener()
    val openLink = onClick ?: defaultOpenLink
    if (node.isCard) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { openLink(node.url) },
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
                        openLink(node.url)
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
internal fun Code(node: ContentNode.Code) {
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
        val effectiveCodeStyle = scaledContentStyle(
            MaterialTheme.typography.bodyMedium,
            maxLineHeight = 1.4f,
        )
        SelectionContainer {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(12.dp),
            ) {
                Text(
                    text = node.content,
                    style = effectiveCodeStyle,
                    fontFamily = CodeFontFamily,
                    softWrap = false,
                )
            }
        }
    }
}

@Composable
internal fun Quote(node: ContentNode.Quote) {
    val indicatorColor = MaterialTheme.colorScheme.outlineVariant
    Column(
        modifier = Modifier
            .drawBehind {
                drawRoundRect(
                    color = indicatorColor,
                    topLeft = Offset.Zero,
                    size = Size(
                        width = 4.dp.toPx(),
                        height = size.height,
                    ),
                    cornerRadius = CornerRadius(2.dp.toPx()),
                )
            }.padding(start = 12.dp),
    ) {
        InlineNodes(
            nodes = node.content,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
internal fun Listing(
    node: ContentNode.Listing,
    depth: Int = 0,
) {
    val indent = 16.dp * depth
    val effectiveLabelStyle = scaledContentStyle(MaterialTheme.typography.bodyLarge)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = indent),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        var index = 0
        node.items.forEach { item ->
            when (item) {
                is ContentNode.Paragraph -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = if (node.isSorted) "${index + 1}." else "•",
                            style = effectiveLabelStyle,
                            modifier = Modifier.alignByBaseline(),
                        )
                        InlineNodes(
                            nodes = item.content,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .weight(1f)
                                .alignByBaseline(),
                        )
                    }
                    index++
                }

                is ContentNode.Listing -> Listing(item, depth + 1)

                else -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = if (node.isSorted) "${index + 1}." else "•",
                            style = effectiveLabelStyle,
                            modifier = Modifier.alignByBaseline(),
                        )
                        RenderContentNode(item)
                    }
                    index++
                }
            }
        }
    }
}
