package com.zhihuminus.feature.comment.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zhihuminus.core.content.EmojiManager
import com.zhihuminus.core.content.renderer.EmojiItem
import com.zhihuminus.feature.comment.Comment
import com.zhihuminus.feature.comment.CommentEvent
import com.zhihuminus.ui.components.replaceSelection

/**
 * 评论输入栏，包含回复目标提示、文本输入框和表情选择面板。
 *
 * @param onEvent 发送事件
 * @param replyToComment 当前回复的目标评论（由外部控制）
 * @param inputFocusRequester 输入框焦点请求器
 */
@Composable
fun CommentInputBar(
    onEvent: (CommentEvent) -> Unit,
    replyToComment: Comment?,
    inputFocusRequester: FocusRequester = remember { FocusRequester() },
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var commentFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    var showEmojiPicker by remember { mutableStateOf(false) }

    Column {
        // 回复目标提示
        AnimatedVisibility(
            visible = replyToComment != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Reply,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "回复 ${replyToComment?.author?.name ?: ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = { onEvent(CommentEvent.DismissReply) },
                        modifier = Modifier.size(24.dp),
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "取消回复",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }
        }

        // 输入框 + 表情面板
        Surface(
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 40.dp, max = 140.dp)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Emoji picker 切换按钮
                    IconButton(
                        onClick = {
                            if (showEmojiPicker) {
                                showEmojiPicker = false
                                inputFocusRequester.requestFocus()
                                keyboardController?.show()
                            } else {
                                focusManager.clearFocus(force = true)
                                keyboardController?.hide()
                                showEmojiPicker = true
                            }
                        },
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            imageVector = if (showEmojiPicker) {
                                Icons.Outlined.Keyboard
                            } else {
                                Icons.Outlined.EmojiEmotions
                            },
                            contentDescription = if (showEmojiPicker) "切换到键盘" else "选择表情",
                            tint = if (showEmojiPicker) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))

                    BasicTextField(
                        value = commentFieldValue,
                        onValueChange = { commentFieldValue = it },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(inputFocusRequester)
                            .onFocusChanged {
                                if (it.isFocused) showEmojiPicker = false
                            },
                        decorationBox = { inner ->
                            Box {
                                if (commentFieldValue.text.isEmpty()) {
                                    Text(
                                        if (replyToComment != null) {
                                            "回复 ${replyToComment.author.name}..."
                                        } else {
                                            "写下你的评论..."
                                        },
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                inner()
                            }
                        },
                        textStyle = TextStyle.Default.copy(
                            fontSize = 16.sp,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                    )

                    IconButton(
                        onClick = {
                            val text = commentFieldValue.text
                            if (text.isNotBlank()) {
                                onEvent(
                                    CommentEvent.SubmitComment(
                                        text = text,
                                        replyToCommentId = replyToComment?.id,
                                    ),
                                )
                                commentFieldValue = TextFieldValue("")
                                onEvent(CommentEvent.DismissReply)
                                showEmojiPicker = false
                            }
                        },
                        enabled = commentFieldValue.text.isNotBlank(),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.Send,
                            contentDescription = "发送评论",
                            tint = if (commentFieldValue.text.isNotBlank()) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            },
                        )
                    }
                }

                // Emoji 选择面板
                AnimatedVisibility(
                    visible = showEmojiPicker,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    if (EmojiManager.mapping.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "暂无可用表情",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 48.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp),
                            contentPadding = PaddingValues(8.dp),
                        ) {
                            items(
                                items = EmojiManager.mapping.entries.toList(),
                                key = { it.key },
                            ) { entry ->
                                val placeholder = entry.key
                                IconButton(
                                    onClick = {
                                        commentFieldValue = commentFieldValue.replaceSelection(
                                            insert = placeholder,
                                            cursorOffsetInInsert = placeholder.length,
                                        )
                                    },
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
            }
        }
    }
}
