package com.zhihuminus.feature.comment

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhihuminus.feature.comment.components.ChildCommentSheet
import com.zhihuminus.feature.post.PostType
import com.zhihuminus.platform.rememberExternalUrlOpener

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

    val openExternalUrl = rememberExternalUrlOpener()

    // 处理副作用
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is CommentEffect.ShowMessage -> {
                    // TODO: 显示 Toast 或 Snackbar
                }

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
    ModalBottomSheet(
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
    ) {
        CommentScreen(
            uiState = viewModel.uiState,
            onEvent = viewModel::onEvent,
            listState = rootListState,
        )
    }

    // 子评论 sheet
    val activeParentId = viewModel.uiState.activeParentId
    if (activeParentId != null) {
        val parentItem = viewModel.uiState.items.find { it.comment.id == activeParentId }
        if (parentItem != null) {
            val children = parentItem.children
            ModalBottomSheet(
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
            ) {
                ChildCommentSheet(
                    parentComment = parentItem.comment,
                    childComments = children?.map { it.comment } ?: emptyList(),
                    isLoading = !parentItem.childrenComplete,
                    isEnd = viewModel.isChildEnd,
                    onLoadMore = { viewModel.loadMoreChildComments() },
                    onEvent = viewModel::onEvent,
                    // 仅当当前 sheet 就是锚点所属的根评论时才传高亮，避免误伤用户手动打开的其他 sheet
                    highlightCommentId = viewModel.uiState.anchorTargetId
                        ?.takeIf { activeParentId == viewModel.uiState.anchorRootId },
                )
            }
        }
    }
}
