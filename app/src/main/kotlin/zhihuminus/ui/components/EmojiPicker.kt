package com.zhihuminus.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.zhihuminus.core.content.EmojiManager
import com.zhihuminus.core.content.renderer.EmojiItem

/**
 * 纯 UI 表情选择网格面板。
 *
 * @param onEmojiClick 点击表情项时的回调，参数为表情占位符（如 "[微笑]"）
 * @param modifier 修饰符
 * @param emojis 表情映射表（键为表情名/占位符，值为资源文件名）
 */
@Composable
fun EmojiPicker(
    onEmojiClick: (String) -> Unit,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(240.dp),
    emojis: Map<String, String> = EmojiManager.mapping,
) {
    if (emojis.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "暂无可用表情",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        val emojiList = remember(emojis) { emojis.entries.toList() }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 48.dp),
            modifier = modifier,
            contentPadding = PaddingValues(8.dp),
        ) {
            items(
                items = emojiList,
                key = { it.key },
            ) { entry ->
                IconButton(
                    onClick = { onEmojiClick(entry.key) },
                    modifier = Modifier.size(48.dp),
                ) {
                    EmojiItem(
                        name = entry.key,
                        resource = entry.value,
                        modifier = Modifier.size(36.dp),
                    )
                }
            }
        }
    }
}

/**
 * 辅助扩展：在当前的 TextFieldValue 选择区域或光标位置插入文本，并将光标移动到插入内容末尾。
 */
fun TextFieldValue.replaceSelection(
    insert: String,
    cursorOffsetInInsert: Int = insert.length,
): TextFieldValue {
    val start = selection.min
    val end = selection.max
    val newText = buildString {
        append(text.substring(0, start))
        append(insert)
        append(text.substring(end))
    }
    val cursor = (start + cursorOffsetInInsert).coerceIn(0, newText.length)
    return TextFieldValue(
        text = newText,
        selection = TextRange(cursor, cursor),
    )
}
