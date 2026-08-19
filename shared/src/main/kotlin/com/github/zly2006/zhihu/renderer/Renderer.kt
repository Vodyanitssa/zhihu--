package com.github.zly2006.zhihu.renderer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import coil3.compose.AsyncImage

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
