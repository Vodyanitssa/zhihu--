package com.zhihuminus.feature.comment

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhihuminus.feature.comment.components.CommentEvent
import com.zhihuminus.feature.comment.components.CommentItem
import com.zhihuminus.feature.post.PostType
import com.zhihuminus.platform.rememberExternalUrlOpener
import com.zhihuminus.platform.rememberImagePreviewOpener
import com.zhihuminus.ui.components.MyModalBottomSheet

/**
 * 评论路由组件，负责创建 ViewModel、处理副作用、展示 ModalBottomSheet。
 *
 * @param showComments 是否显示评论
 * @param onDismiss 关闭评论
 * @param contentType 内容类型
 * @param contentId 内容 ID
 * @param repository 评论仓库
 * @param initialCommentId 深链锚点评论 ID（可选）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentRoute(
    showComments: Boolean,
    onDismiss: () -> Unit,
    contentType: PostType,
    contentId: Long,
    repository: CommentRepository,
    initialCommentId: String? = null,
) {
    if (!showComments) return

    val viewModel: CommentViewModel = viewModel(key = "comments_${contentType}_$contentId") {
        CommentViewModel(
            contentType = contentType,
            contentId = contentId,
            repository = repository,
            initialCommentId = initialCommentId,
        )
    }

    val openImagePreview = rememberImagePreviewOpener()
    val openExternalUrl = rememberExternalUrlOpener()

    // 处理副作用
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is CommentEffect.ShowMessage -> {
                    // TODO: 显示 Toast 或 Snackbar
                }

                is CommentEffect.OpenImage -> openImagePreview(effect.url)
                is CommentEffect.OpenExternalUrl -> openExternalUrl(effect.url)
                is CommentEffect.ScrollToTop -> {
                    // 由 CommentScreen 内部处理
                }
            }
        }
    }

    val rootSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val childSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val rootListState = rememberLazyListState()

    // 根评论 sheet
    MyModalBottomSheet(
        onDismissRequest = {
            viewModel.onEvent(CommentEvent.DismissChildComments)
            onDismiss()
        },
        sheetState = rootSheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        properties = ModalBottomSheetProperties(
            shouldDismissOnBackPress = true,
            shouldDismissOnClickOutside = true,
        ),
        dragHandle = {
            Column {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "评论",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    fontSize = 18.sp,
                    lineHeight = 26.sp,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        },
        usePlatformWindow = true,
    ) {
        CommentScreen(
            uiState = viewModel.uiState,
            onEvent = viewModel::onEvent,
            listState = rootListState,
        )
    }

    // 子评论 sheet
    val activeParent = viewModel.activeParentComment
    if (activeParent != null) {
        MyModalBottomSheet(
            onDismissRequest = {
                viewModel.onEvent(CommentEvent.DismissChildComments)
            },
            sheetState = childSheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            properties = ModalBottomSheetProperties(
                shouldDismissOnBackPress = true,
                shouldDismissOnClickOutside = true,
            ),
            dragHandle = {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "回复",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        fontSize = 18.sp,
                        lineHeight = 26.sp,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            },
            usePlatformWindow = true,
        ) {
            ChildCommentSheet(
                parentComment = activeParent,
                childComments = viewModel.childComments,
                isLoading = viewModel.isChildLoading,
                isEnd = viewModel.isChildEnd,
                onLoadMore = { viewModel.loadMoreChildComments() },
                onLike = { commentId, liked ->
                    if (liked) {
                        viewModel.onEvent(CommentEvent.UnlikeComment(commentId))
                    } else {
                        viewModel.onEvent(CommentEvent.LikeComment(commentId))
                    }
                },
                onDelete = { viewModel.onEvent(CommentEvent.DeleteComment(it)) },
            )
        }
    }
}

/**
 * 子评论列表（第二个 bottom sheet 的内容）
 */
@Composable
private fun ChildCommentSheet(
    parentComment: Comment,
    childComments: List<Comment>,
    isLoading: Boolean,
    isEnd: Boolean,
    onLoadMore: () -> Unit,
    onLike: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
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
                    showSubComments = false,
                )
            }

            // 回复数栏
            item(key = "reply_header") {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "回复 ${parentComment.childCommentCount}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
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
                        showSubComments = false,
                        onLike = { onLike(comment.id, comment.liked) },
                        onDelete = { onDelete(comment.id) },
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
