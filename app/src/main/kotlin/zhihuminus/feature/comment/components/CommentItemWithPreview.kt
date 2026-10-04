package com.zhihuminus.feature.comment.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zhihuminus.feature.comment.CommentEvent
import com.zhihuminus.feature.comment.CommentItemUiState

/**
 * 带子评论预览的评论条目。
 * 根评论默认展示最多2条子评论预览，未展示完则显示"查看子评论"按钮。
 */
@Composable
fun CommentItemWithPreview(
    item: CommentItemUiState,
    onEvent: (CommentEvent) -> Unit,
) {
    Column {
        CommentItem(
            comment = item.comment,
            onEvent = onEvent,
        )

        // 子评论预览（与子评论 sheet 共用 UI 树 children，同一数据源）
        val previews = item.children.orEmpty()
        val displayedPreviews = previews.take(2)
        val totalCount = item.comment.childCommentCount

        val hasContentToShow = displayedPreviews.isNotEmpty() || totalCount > 0
        val hasMoreChildren = totalCount > displayedPreviews.size

        if (hasContentToShow) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 40.dp, top = 8.dp),
            ) {
                displayedPreviews.forEach { child ->
                    CommentItem(
                        comment = child.comment,
                        onEvent = onEvent,
                    )
                }
                if (hasMoreChildren) {
                    Button(
                        onClick = { onEvent(CommentEvent.OpenChildComments(item.comment)) },
                        modifier = Modifier.height(28.dp),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.Comment,
                            contentDescription = "查看回复",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "查看 $totalCount 条回复",
                            fontSize = 12.sp,
                            lineHeight = 12.sp,
                        )
                    }
                }
            }
        }
    }
}
