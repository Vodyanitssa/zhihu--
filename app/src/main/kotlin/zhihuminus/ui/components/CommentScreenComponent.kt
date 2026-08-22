/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.zhihuminus.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zhihuminus.data.DataHolder
import com.zhihuminus.data.ZhihuJson
import com.zhihuminus.navigation.Article
import com.zhihuminus.navigation.CommentHolder
import com.zhihuminus.navigation.NavDestination
import com.zhihuminus.navigation.Pin
import com.zhihuminus.navigation.Question
import com.zhihuminus.navigation.SegmentCommentHolder
import com.zhihuminus.platform.rememberSettingsStore
import com.zhihuminus.theme.Typography
import com.zhihuminus.ui.CommentScreen
import com.zhihuminus.ui.rememberArticleHost
import com.zhihuminus.viewmodel.CommentItem

/**
 * 最好不要在 if 或者其他条件语句中使用，这会导致本组件内部状态丢失。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentScreenComponent(
    showComments: Boolean,
    onDismiss: () -> Unit,
    content: NavDestination,
    isZhPlusAuthorContent: Boolean = false,
) {
    val settings = rememberSettingsStore()
    val articleHost = rememberArticleHost()
    var pendingCommentId by remember(content) {
        mutableStateOf(articleHost?.consumePendingCommentId(content))
    }
    val commentsVisible = showComments || pendingCommentId != null
    var authorCommentPolicyAcknowledged by remember {
        mutableStateOf(settings.getBoolean(ZH_PLUS_AUTHOR_COMMENT_POLICY_ACKNOWLEDGED_KEY, false))
    }
    val contentStateKey = commentContentStateKey(content)
    var activeChildComment by rememberSaveable(contentStateKey, saver = activeChildCommentSaver) {
        mutableStateOf<CommentItem?>(null)
    }
    var pendingChildComment by remember(contentStateKey) {
        mutableStateOf<DataHolder.Comment?>(null)
    }
    var commentDrafts by rememberSaveable(contentStateKey) {
        mutableStateOf<Map<String, String>>(emptyMap())
    }
    var rootListResetToken by rememberSaveable(contentStateKey) { mutableIntStateOf(0) }
    val rootListState = rememberSaveable(
        contentStateKey,
        rootListResetToken,
        saver = LazyListState.Saver,
    ) {
        LazyListState()
    }
    val rootSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val childSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val childTarget = activeChildComment?.clickTarget
    val childDraftKey = childTarget?.let(::commentContentStateKey)
    var childListResetToken by rememberSaveable(contentStateKey) { mutableIntStateOf(0) }
    val childListState = rememberSaveable(
        contentStateKey,
        childDraftKey,
        childListResetToken,
        saver = LazyListState.Saver,
    ) {
        LazyListState()
    }

    @Composable
    fun DragHandleTitle(text: String) {
        Column {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text,
                style = Typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                ),
                textAlign = TextAlign.Center,
                fontSize = 18.sp,
                lineHeight = 26.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    fun dismissRootComments() {
        pendingCommentId = null
        pendingChildComment = null
        activeChildComment = null
        rootListResetToken += 1
        childListResetToken += 1
        onDismiss()
    }

    fun updateCommentDraft(key: String, value: String) {
        commentDrafts = if (value.isEmpty()) {
            commentDrafts - key
        } else {
            commentDrafts + (key to value)
        }
    }

    if (commentsVisible) {
        MyModalBottomSheet(
            onDismissRequest = { dismissRootComments() },
            sheetState = rootSheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            properties = ModalBottomSheetProperties(
                shouldDismissOnBackPress = true,
                shouldDismissOnClickOutside = true,
            ),
            dragHandle = { DragHandleTitle("评论") },
            usePlatformWindow = content !is Article,
        ) {
            CommentScreen(
                content = { content },
                initialCommentId = pendingCommentId,
                onChildCommentClick = { commentItem ->
                    if (commentItem.clickTarget != null) {
                        pendingChildComment = null
                        activeChildComment = commentItem
                    }
                },
                commentInput = commentDrafts[contentStateKey].orEmpty(),
                onCommentInputChange = { updateCommentDraft(contentStateKey, it) },
                listState = rootListState,
                onInitialChildCommentResolved = { rootComment, childComment ->
                    pendingChildComment = childComment
                    activeChildComment = rootComment
                },
            )
        }
    }

    if (commentsVisible && activeChildComment != null && childTarget != null && childDraftKey != null) {
        MyModalBottomSheet(
            onDismissRequest = {
                pendingChildComment = null
                activeChildComment = null
                childListResetToken += 1
            },
            sheetState = childSheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            properties = ModalBottomSheetProperties(
                shouldDismissOnBackPress = true,
                shouldDismissOnClickOutside = true,
            ),
            dragHandle = { DragHandleTitle("回复") },
            usePlatformWindow = childTarget.article !is Article,
        ) {
            CommentScreen(
                content = { childTarget },
                activeCommentItem = activeChildComment,
                onChildCommentClick = { },
                commentInput = commentDrafts[childDraftKey].orEmpty(),
                onCommentInputChange = { updateCommentDraft(childDraftKey, it) },
                listState = childListState,
                initialComment = pendingChildComment,
            )
        }
    }
}

const val ZH_PLUS_AUTHOR_COMMENT_POLICY_ACKNOWLEDGED_KEY = "zhPlusAuthorCommentPolicyAcknowledged"

private val activeChildCommentSaver = Saver<MutableState<CommentItem?>, List<String>>(
    save = { state ->
        val commentItem = state.value
        val target = commentItem?.clickTarget
        if (commentItem == null || target == null) {
            emptyList()
        } else {
            listOf(
                ZhihuJson.json.encodeToString(commentItem.item),
                ZhihuJson.json.encodeToString(target),
            )
        }
    },
    restore = { saved ->
        mutableStateOf(
            if (saved.size != 2) {
                null
            } else {
                runCatching {
                    CommentItem(
                        item = ZhihuJson.json.decodeFromString<DataHolder.Comment>(saved[0]),
                        clickTarget = ZhihuJson.json.decodeFromString<CommentHolder>(saved[1]),
                    )
                }.getOrNull()
            },
        )
    },
)

private fun commentContentStateKey(content: NavDestination): String = when (content) {
    is Article -> "article:${content.type}:${content.id}"
    is Question -> "question:${content.questionId}"
    is Pin -> "pin:${content.id}"
    is CommentHolder -> "comment:${commentContentStateKey(content.article)}:${content.commentId}"
    is SegmentCommentHolder -> "segment:${content.contentType}:${content.contentId}:${content.segmentId}"
    else -> content.toString()
}
