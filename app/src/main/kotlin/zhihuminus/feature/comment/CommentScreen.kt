package com.zhihuminus.feature.comment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import com.zhihuminus.feature.comment.components.CommentInputBar
import com.zhihuminus.feature.comment.components.CommentItemWithPreview
import com.zhihuminus.feature.comment.components.SortBar

data class CommentItemUiState(
    val comment: Comment,
    val children: List<CommentItemUiState>? = null, // null=未加载子评论, []=已加载无子评论
    val childrenComplete: Boolean = false, // true=通过子评论接口加载的完整列表; false=仅预览数据或加载中占位
)

data class CommentUiState(
    val items: List<CommentItemUiState> = emptyList(),
    val sortOrder: CommentSortOrder = CommentSortOrder.SCORE,
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val isEnd: Boolean = false,
    val errorMessage: String? = null,
    val activeParentId: String? = null, // 当前打开子sheet的父评论ID
    val replyToComment: Comment? = null, // 当前回复的目标评论
    val anchorRootId: String? = null, // 深链锚点：目标根评论 ID，用于滚动定位
    val anchorTargetId: String? = null, // 深链锚点：高亮目标评论 ID（锚点为子评论时非空）
)

@Composable
fun CommentScreen(
    uiState: CommentUiState,
    onEvent: (CommentEvent) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    inputFocusRequester: FocusRequester = remember { FocusRequester() },
    commentInput: String = "",
    onCommentInputChange: ((String) -> Unit)? = null,
) {
    // 当 replyToComment 从 null 变为非 null 时，自动聚焦输入框
    LaunchedEffect(uiState.replyToComment) {
        if (uiState.replyToComment != null) {
            inputFocusRequester.requestFocus()
        }
    }

    // 滚动加载更多
    val shouldLoadMore = remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= layoutInfo.totalItemsCount - 3 &&
                !uiState.isLoading &&
                !uiState.isLoadingMore &&
                !uiState.isEnd
        }
    }
    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) {
            onEvent(CommentEvent.LoadMore)
        }
    }

    // 深链锚点：根评论列表就绪后滚动定位（排序栏占 index 0，故 +1）
    var anchorScrolled by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.anchorRootId, uiState.items) {
        val rootId = uiState.anchorRootId ?: return@LaunchedEffect
        if (anchorScrolled) return@LaunchedEffect
        val index = uiState.items.indexOfFirst { it.comment.id == rootId }
        if (index >= 0) {
            anchorScrolled = true
            listState.scrollToItem(index + 1)
        }
    }

    Box(Modifier.fillMaxSize()) {
        Surface(
            modifier = modifier.fillMaxSize(),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
            ) {
                // 评论列表区域
                Box(modifier = Modifier.weight(1f)) {
                    when {
                        uiState.isLoading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        uiState.errorMessage != null -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(uiState.errorMessage, color = MaterialTheme.colorScheme.error)
                            }
                        }

                        uiState.items.isEmpty() && !uiState.isLoadingMore -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("暂无评论", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        else -> {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    bottom = 16.dp,
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 8.dp,
                                ),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                // 排序切换栏
                                item(key = "sorting") {
                                    SortBar(
                                        sortOrder = uiState.sortOrder,
                                        onSortChange = { onEvent(CommentEvent.ChangeSortOrder(it)) },
                                    )
                                }

                                // 评论列表（递归渲染）
                                uiState.items.forEach { item ->
                                    item(key = item.comment.id) {
                                        CommentItemWithPreview(
                                            item = item,
                                            onEvent = onEvent,
                                        )
                                    }
                                }

                                // 加载更多指示器
                                if (uiState.isLoadingMore) {
                                    item(key = "loading") {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(8.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                strokeWidth = 2.dp,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 评论输入栏
                CommentInputBar(
                    onEvent = onEvent,
                    replyToComment = uiState.replyToComment,
                    inputFocusRequester = inputFocusRequester,
                    initialDraft = commentInput,
                    onDraftChange = onCommentInputChange,
                )
            }
        }
    }
}
