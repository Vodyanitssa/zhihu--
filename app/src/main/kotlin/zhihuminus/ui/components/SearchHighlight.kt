package com.zhihuminus.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

private val EM_TAG_REGEX = Regex("<em>(.*?)</em>", RegexOption.DOT_MATCHES_ALL)

/**
 * 将知乎服务端返回的带 <em> 搜索高亮标签的文本解析为带强调颜色的 [AnnotatedString]。
 */
@Composable
fun searchHighlightedText(
    text: String,
    highlightColor: Color = MaterialTheme.colorScheme.primary,
): AnnotatedString = remember(text, highlightColor) {
    if (!text.contains("<em>")) {
        AnnotatedString(text)
    } else {
        buildAnnotatedString {
            var cursor = 0
            EM_TAG_REGEX.findAll(text).forEach { match ->
                append(text.substring(cursor, match.range.first))
                withStyle(SpanStyle(color = highlightColor)) {
                    append(match.groupValues[1])
                }
                cursor = match.range.last + 1
            }
            if (cursor < text.length) {
                append(text.substring(cursor))
            }
        }
    }
}
