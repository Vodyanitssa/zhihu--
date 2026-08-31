package com.zhihuminus.feature.comment.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import com.zhihuminus.feature.comment.Comment
import com.zhihuminus.feature.comment.CommentEvent
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * 子评论列表（第二个 bottom sheet 的内容）
 */
@Composable
fun ChildCommentSheet(
    parentComment: Comment,
    childComments: List<Comment>,
    isLoading: Boolean,
    isEnd: Boolean,
    onLoadMore: () -> Unit,
    onEvent: (CommentEvent) -> Unit,
    highlightCommentId: String? = null,
) {
    val listState = rememberLazyListState()

    // 滚动加载更多
    val shouldLoadMore = remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= layoutInfo.totalItemsCount - 3 && !isLoading && !isEnd
        }
    }
    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) onLoadMore()
    }

    // 深链锚点：定位目标子评论。列表增量加载期间持续尝试（size 变化重启本 effect），最多定位一次
    var anchorLocated by remember(highlightCommentId) { mutableStateOf(false) }
    var highlightVisible by remember(highlightCommentId) { mutableStateOf(highlightCommentId != null) }
    LaunchedEffect(highlightCommentId, childComments.size) {
        val targetId = highlightCommentId ?: return@LaunchedEffect
        if (anchorLocated) return@LaunchedEffect
        val index = childComments.indexOfFirst { it.id == targetId }
        if (index < 0) return@LaunchedEffect
        anchorLocated = true
        // 父评论、回复数栏各占一行，故 +2
        listState.scrollToItem(index + 2)
    }

    // 定位成功后高亮 3 秒再消费锚点；全量加载完仍未命中（评论已删除等）则兜底消费
    LaunchedEffect(highlightCommentId, anchorLocated, isLoading, isEnd) {
        val targetId = highlightCommentId ?: return@LaunchedEffect
        if (!anchorLocated) {
            if (!isLoading && isEnd) {
                highlightVisible = false
                onEvent(CommentEvent.ConsumeAnchor)
            }
            return@LaunchedEffect
        }
        delay(3000.milliseconds)
        highlightVisible = false
        onEvent(CommentEvent.ConsumeAnchor)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp, start = 16.dp, end = 16.dp, top = 8.dp),
        ) {
            // 父评论
            item(key = "parent_${parentComment.id}") {
                CommentItem(
                    comment = parentComment,
                    onEvent = onEvent,
                )
            }

            // 回复数栏
            item(key = "reply_header") {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "回复 ${parentComment.childCommentCount}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
            }

            // 子评论列表
            if (childComments.isEmpty() && !isLoading) {
                item(key = "empty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("暂无回复", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(
                    items = childComments,
                    key = { it.id },
                ) { comment ->
                    CommentItem(
                        comment = comment,
                        onEvent = onEvent,
                        highlight = highlightVisible && comment.id == highlightCommentId,
                    )
                }
            }

            if (isLoading) {
                item(key = "loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                }
            }
        }
    }
}
