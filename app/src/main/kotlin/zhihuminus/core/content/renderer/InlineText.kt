package com.zhihuminus.core.content.renderer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.takeOrElse
import coil3.compose.AsyncImage
import com.zhihuminus.core.content.ContentNode
import com.zhihuminus.core.content.EmojiManager
import com.zhihuminus.core.content.FormulaManager
import com.zhihuminus.core.content.InlineNode
import com.zhihuminus.core.platform.copyText
import com.zhihuminus.navigation.link.rememberInAppLinkOpener
import com.zhihuminus.theme.CodeFontFamily
import kotlinx.coroutines.launch

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
    applyTypographyConfig: Boolean = true,
) {
    val effectiveStyle = if (applyTypographyConfig) {
        scaledContentStyle(style)
    } else {
        style
    }
    var activeFootnote by remember { mutableStateOf<InlineNode.Footnote?>(null) }
    val openInApp = rememberInAppLinkOpener()
    val linkStyle = TextLinkStyles(
        style = SpanStyle(
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
        ),
    )
    val footnoteStyle = TextLinkStyles(
        style = SpanStyle(
            color = MaterialTheme.colorScheme.primary,
            baselineShift = BaselineShift.Superscript,
            fontSize = 0.8.em,
            fontWeight = FontWeight.Medium,
        ),
    )
    val codeStyle = MaterialTheme.colorScheme.surfaceVariant
    // 行内公式与表情的 InlineTextContent；公式的尺寸解析与占位符构建在 FormulaManager。
    // BoxWithConstraints 提供可用宽度：超宽公式强制占满可用宽度、高度等比缩放
    val emSize = with(LocalDensity.current) { effectiveStyle.fontSize.takeOrElse { 16.sp }.toDp() }
    val text = buildAnnotatedString {
        fun appendNode(node: InlineNode) {
            when (node) {
                is InlineNode.Text -> append(node.text)

                is InlineNode.Emoji -> appendInlineContent(node.name)

                is InlineNode.Bold -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    node.children.forEach(::appendNode)
                }

                is InlineNode.Italic -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    node.children.forEach(::appendNode)
                }

                is InlineNode.Code -> withStyle(
                    SpanStyle(
                        fontFamily = CodeFontFamily,
                        background = codeStyle,
                    ),
                ) {
                    append(node.text)
                }

                is InlineNode.Formula -> appendInlineContent(node.url, "[公式]")

                is InlineNode.Link -> {
                    val link = LinkAnnotation.Url(
                        url = node.url,
                        styles = linkStyle,
                    ) {
                        val url = (it as LinkAnnotation.Url).url
                        openInApp(url)
                    }
                    withLink(link) {
                        append(node.name)
                    }
                }

                is InlineNode.Footnote -> {
                    val link = LinkAnnotation.Clickable(
                        tag = "footnote",
                        styles = footnoteStyle,
                    ) {
                        activeFootnote = node
                    }
                    withLink(link) {
                        append(node.label)
                    }
                }

                is InlineNode.LineBreak -> appendLine()
            }
        }

        nodes.forEach(::appendNode)
    }
    BoxWithConstraints {
        val inlineContent = EmojiManager.inlineContent +
            FormulaManager.formulaInlineContent(nodes, emSize, maxWidth)
        Text(
            text = text,
            modifier = modifier,
            style = effectiveStyle,
            color = color,
            maxLines = maxLines,
            overflow = overflow,
            inlineContent = inlineContent,
        )
    }

    activeFootnote?.let { footnote ->
        FootnoteDialog(
            footnote = footnote,
            onDismiss = { activeFootnote = null },
            onOpenUrl = { url ->
                activeFootnote = null
                openInApp(url)
            },
        )
    }
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
private fun FootnoteDialog(
    footnote: InlineNode.Footnote,
    onDismiss: () -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    val hasUrl = footnote.url.isNotBlank()
    val hasText = footnote.text.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (footnote.label.isNotBlank()) "注释 ${footnote.label}" else "注释",
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (!hasText && !hasUrl) {
                    Text(
                        text = "暂无注释详情",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (hasText) {
                    SelectionContainer {
                        Text(
                            text = footnote.text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                if (hasUrl) {
                    Link(
                        node = ContentNode.Link(
                            content = "参考链接",
                            url = footnote.url,
                            isCard = true,
                        ),
                        onClick = onOpenUrl,
                    )
                }
            }
        },
        confirmButton = {
            when {
                hasUrl -> Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                clipboard.copyText(footnote.url)
                            }
                        },
                    ) {
                        Text("复制链接")
                    }
                    TextButton(onClick = { onOpenUrl(footnote.url) }) {
                        Text("打开链接")
                    }
                }

                hasText -> TextButton(
                    onClick = {
                        coroutineScope.launch {
                            clipboard.copyText(footnote.text)
                        }
                    },
                ) {
                    Text("复制内容")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        },
    )
}
