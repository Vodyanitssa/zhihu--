package com.github.zly2006.zhihu.renderer

import androidx.compose.foundation.text.InlineTextContent

sealed interface ContentNode {
    data class Paragraph(
        val content: InlineNode,
    ) : ContentNode

    data class Heading(
        val level: Int,
        val content: InlineTextContent,
    ) : ContentNode

    data class Image(
        val url: String,
        val caption: String?,
    ) : ContentNode

    data class Code(
        val code: String,
        val language: String?,
    ) : ContentNode

    data class Quote(
        val content: InlineTextContent,
    ) : ContentNode

    data class List(
        val items: kotlin.collections.List<InlineTextContent>,
        val ordered: Boolean,
    ) : ContentNode
}

sealed interface InlineNode {
    data class Text(
        val text: String,
    ) : InlineNode

    data class Emoji(
        val name: String,
    ) : InlineNode
}

fun InlineNodeParser(text: String): List<InlineNode> {
    val result = mutableListOf<InlineNode>()
    val textBuffer = StringBuilder()
    var i = 0
    while (i < text.length) {
        if (text[i] == '[') {
            val closeIdx = text.indexOf(']', i + 1)
            if (closeIdx != -1) {
                val key = text.substring(i, closeIdx + 1)
                if (EmojiManager.mapping.containsKey(key)) {
                    if (textBuffer.isNotEmpty()) {
                        result.add(InlineNode.Text(textBuffer.toString()))
                        textBuffer.clear()
                    }
                    result.add(
                        InlineNode.Emoji(
                            name = key,
                        ),
                    )
                    i = closeIdx + 1
                    continue
                }
            }
        }
        textBuffer.append(text[i])
        i++
    }
    if (textBuffer.isNotEmpty()) {
        result.add(InlineNode.Text(textBuffer.toString()))
    }
    return result
}
