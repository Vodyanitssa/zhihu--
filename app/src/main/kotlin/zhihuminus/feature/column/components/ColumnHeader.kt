package com.zhihuminus.feature.column.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zhihuminus.core.error.AppError
import com.zhihuminus.core.util.formatRelativeTime
import com.zhihuminus.feature.column.ColumnDetail
import com.zhihuminus.ui.components.ExpandableContent
import com.zhihuminus.ui.components.FollowButton
import com.zhihuminus.ui.components.InfoHeader
import com.zhihuminus.ui.components.InfoStatItem
import com.zhihuminus.ui.components.collapsedTextHeight

@Composable
fun ColumnHeader(
    detail: ColumnDetail?,
    detailErrorMessage: AppError?,
    isFollowingChanging: Boolean,
    isIntroductionExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onRetryDetail: () -> Unit,
    onFollowingChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    InfoHeader(
        modifier = modifier,
        iconUrl = detail?.avatarUrl,
        iconShape = RoundedCornerShape(12.dp),
        iconBackgroundColor = MaterialTheme.colorScheme.surfaceVariant,
        iconContentDescription = "专栏头像",
        title = detail?.title?.takeIf { it.isNotBlank() } ?: "专栏",
        stats = {
            when {
                detail != null -> ColumnStats(detail)
                detailErrorMessage != null -> TextButton(onClick = onRetryDetail) {
                    Text(
                        "信息加载失败，点击重试",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                else -> Text(
                    "正在加载专栏信息…",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        actions = if (detail != null) {
            {
                FollowButton(
                    isFollowing = detail.isFollowing,
                    onClick = { onFollowingChange(!detail.isFollowing) },
                    isLoading = isFollowingChanging,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            null
        },
        introduction = {
            detail?.description?.takeIf { it.isNotBlank() }?.let { description ->
                ExpandableContent(
                    isExpanded = isIntroductionExpanded,
                    onExpandedChange = onExpandedChange,
                    collapsedMaxHeight = collapsedTextHeight(10, MaterialTheme.typography.bodyMedium),
                ) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        footer = {
            detail?.let { loadedDetail ->
                val createdText = remember(loadedDetail.createdAt) { formatRelativeTime(loadedDetail.createdAt) }
                if (createdText.isNotBlank()) {
                    Text(
                        text = "创建于$createdText",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        },
    )
}

@Composable
private fun ColumnStats(detail: ColumnDetail) {
    detail.articleCount.takeIf { it > 0 }?.let {
        InfoStatItem(label = "文章", value = it.toLong())
    }
    detail.followerCount.takeIf { it > 0 }?.let {
        InfoStatItem(label = "关注", value = it.toLong())
    }
    detail.voteupCount.takeIf { it > 0 }?.let {
        InfoStatItem(label = "赞", value = it.toLong())
    }
}
