package com.zhihuminus.feature.comment.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zhihuminus.feature.comment.Comment
import com.zhihuminus.feature.comment.CommentEvent
import com.zhihuminus.ui.components.EmojiPicker
import com.zhihuminus.ui.components.replaceSelection
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * 评论输入浮层（置于同一个 BottomSheet 内的最顶层，避免多窗口 Dialog 产生系统遮罩冲突）。
 *
 * 结构：
 * - 透明点击背景（点击关闭输入框）
 * - 底部页面（Surface 容器），支持弹簧动画平滑扩展到屏幕顶层
 * - 嵌套文本框，右上角附带放大/还原按钮，回复目标以灰色占位文字显示
 * - 文本框下方单行操作栏（表情按钮 + 发送按钮）
 * - 底部按需展开表情选择面板
 */
@Composable
fun CommentInputOverlay(
    onDismiss: () -> Unit,
    onEvent: (CommentEvent) -> Unit,
    replyToComment: Comment?,
    initialDraft: String = "",
    onDraftChange: ((String) -> Unit)? = null,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val inputFocusRequester = remember { FocusRequester() }

    var commentFieldValue by remember { mutableStateOf(TextFieldValue(initialDraft)) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    fun updateDraft(value: TextFieldValue) {
        commentFieldValue = value
        onDraftChange?.invoke(value.text)
    }

    LaunchedEffect(Unit) {
        delay(150.milliseconds)
        inputFocusRequester.requestFocus()
        keyboardController?.show()
    }

    // 拦截物理/手势返回键，关闭输入浮层
    BackHandler(enabled = true, onBack = onDismiss)

    Box(modifier = Modifier.fillMaxSize()) {
        // 背景半透明遮罩，点击关闭弹窗
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
        )

        // 底部页面浮层区域（处理输入法及系统栏避让）
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            val screenMaxHeight = maxHeight
            val baseCollapsedHeight = 200.dp
            val collapsedHeight = (baseCollapsedHeight + if (showEmojiPicker) 240.dp else 0.dp)
                .coerceAtMost(screenMaxHeight)
            val targetHeight = if (isExpanded) screenMaxHeight else collapsedHeight

            // 全屏展开与折叠的平滑过渡动画
            val animatedHeight by animateDpAsState(
                targetValue = targetHeight,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
                label = "sheetHeight",
            )

            val animatedCornerRadius by animateDpAsState(
                targetValue = if (isExpanded) 0.dp else 16.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
                label = "sheetCornerRadius",
            )

            Surface(
                shape = RoundedCornerShape(
                    topStart = animatedCornerRadius,
                    topEnd = animatedCornerRadius,
                ),
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(animatedHeight),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    // 嵌套文本框容器
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                        ) {
                            BasicTextField(
                                value = commentFieldValue,
                                onValueChange = ::updateDraft,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(end = 40.dp)
                                    .focusRequester(inputFocusRequester)
                                    .onFocusChanged {
                                        if (it.isFocused) showEmojiPicker = false
                                    },
                                textStyle = TextStyle.Default.copy(
                                    fontSize = 16.sp,
                                    lineHeight = 22.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                decorationBox = { innerTextField ->
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        if (commentFieldValue.text.isEmpty()) {
                                            Text(
                                                text = if (replyToComment != null) {
                                                    "回复 @${replyToComment.author.name}..."
                                                } else {
                                                    "写下你的评论..."
                                                },
                                                fontSize = 16.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        innerTextField()
                                    }
                                },
                            )

                            // 文本框右上角放大按钮
                            IconButton(
                                onClick = { isExpanded = !isExpanded },
                                modifier = Modifier
                                    .size(36.dp)
                                    .align(Alignment.TopEnd),
                            ) {
                                Icon(
                                    imageVector = if (isExpanded) {
                                        Icons.Default.FullscreenExit
                                    } else {
                                        Icons.Default.Fullscreen
                                    },
                                    contentDescription = if (isExpanded) "还原" else "放大",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 文本框下面是一行内容
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // 表情切换按钮
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

                        Spacer(modifier = Modifier.weight(1f))

                        // 发送按钮
                        val isSendEnabled = commentFieldValue.text.isNotBlank()
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
                                    onDraftChange?.invoke("")
                                    showEmojiPicker = false
                                }
                            },
                            enabled = isSendEnabled,
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Send,
                                contentDescription = "发送",
                                tint = if (isSendEnabled) {
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
                        EmojiPicker(
                            onEmojiClick = { placeholder ->
                                updateDraft(
                                    commentFieldValue.replaceSelection(
                                        insert = placeholder,
                                        cursorOffsetInInsert = placeholder.length,
                                    ),
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}
