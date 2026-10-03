package com.zhihuminus.feature.notification

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.zhihuminus.feature.notification.components.PrivateMessageBubble
import com.zhihuminus.platform.PlatformBackHandler
import com.zhihuminus.ui.components.EmojiPicker
import com.zhihuminus.ui.components.PaginatedList
import com.zhihuminus.ui.components.ProgressIndicatorFooter
import com.zhihuminus.ui.components.replaceSelection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivateMessageScreen(
    state: PrivateMessageUiState,
    peerName: String,
    peerAvatar: String,
    onEvent: (PrivateMessageEvent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val inputFocusRequester = remember { FocusRequester() }
    var messageFieldValue by rememberSaveable(
        state.peerId,
        stateSaver = TextFieldValue.Saver,
    ) {
        mutableStateOf(TextFieldValue())
    }
    var showEmojiPicker by rememberSaveable { mutableStateOf(false) }

    PlatformBackHandler(enabled = showEmojiPicker) {
        showEmojiPicker = false
    }

    val timestampVisibleIds = remember(state.messages) {
        calculateTimestampVisibleIds(state.messages)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (peerAvatar.isNotBlank()) {
                            AsyncImage(
                                model = peerAvatar,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            text = peerName.ifBlank { "私信" },
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = messageFieldValue,
                            onValueChange = { messageFieldValue = it },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(inputFocusRequester)
                                .onFocusChanged {
                                    if (it.isFocused) {
                                        showEmojiPicker = false
                                    }
                                },
                            placeholder = { Text("发私信") },
                            enabled = !state.isSending,
                            maxLines = 4,
                            shape = RoundedCornerShape(12.dp),
                        )
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
                            enabled = !state.isSending,
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
                        IconButton(
                            onClick = {
                                val content = messageFieldValue.text
                                onEvent(PrivateMessageEvent.SendMessage(content))
                                messageFieldValue = TextFieldValue("")
                                showEmojiPicker = false
                            },
                            enabled = messageFieldValue.text.isNotBlank() && !state.isSending,
                        ) {
                            Icon(
                                Icons.AutoMirrored.Outlined.Send,
                                contentDescription = "发送",
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = showEmojiPicker,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        EmojiPicker(
                            onEmojiClick = { placeholder ->
                                messageFieldValue = messageFieldValue.replaceSelection(
                                    insert = placeholder,
                                    cursorOffsetInInsert = placeholder.length,
                                )
                            },
                        )
                    }
                }
            }
        },
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { onEvent(PrivateMessageEvent.Refresh) },
            modifier = Modifier.padding(paddingValues),
        ) {
            PaginatedList(
                items = state.messages,
                onLoadMore = { onEvent(PrivateMessageEvent.LoadMore) },
                isEnd = { state.isEnd },
                reverseLayout = true,
                verticalArrangement = Arrangement.Top,
                showEndNotice = false,
                contentPadding = PaddingValues(vertical = 12.dp),
                modifier = Modifier.fillMaxSize(),
                footer = if (state.isRefreshing) null else ProgressIndicatorFooter,
                key = { it.stableId },
            ) { message ->
                PrivateMessageBubble(
                    message = message,
                    incoming = message.sender?.let { sender ->
                        sender.id == state.peerId || sender.urlToken == state.peerId
                    } == true,
                    showTimestamp = message.stableId in timestampVisibleIds,
                )
            }
        }
    }
}

internal const val PRIVATE_MESSAGE_TIMESTAMP_COLLAPSE_INTERVAL_SECONDS = 120L

internal fun calculateTimestampVisibleIds(messages: List<PrivateMessage>): Set<String> {
    val visibleIds = mutableSetOf<String>()
    var lastDisplayedTime: Long? = null

    for (i in messages.indices.reversed()) {
        val message = messages[i]
        val time = message.createdTime
        if (time <= 0) continue

        val lastTime = lastDisplayedTime
        if (lastTime == null || kotlin.math.abs(time - lastTime) >= PRIVATE_MESSAGE_TIMESTAMP_COLLAPSE_INTERVAL_SECONDS) {
            visibleIds.add(message.stableId)
            lastDisplayedTime = time
        }
    }
    return visibleIds
}
