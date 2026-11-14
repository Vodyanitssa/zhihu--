package com.zhihuminus.feature.topic.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.zhihuminus.core.error.AppError
import com.zhihuminus.feature.topic.TopicDetail
import com.zhihuminus.ui.components.ExpandableContent
import com.zhihuminus.ui.components.FollowButton
import com.zhihuminus.ui.components.InfoHeader
import com.zhihuminus.ui.components.InfoStatItem
import com.zhihuminus.ui.components.collapsedTextHeight

@Composable
fun TopicHeader(
    detail: TopicDetail?,
    detailErrorMessage: AppError?,
    topicName: String,
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
        iconShape = CircleShape,
        iconContentDescription = "话题头像",
        title = detail?.name?.ifBlank { topicName } ?: topicName,
        stats = {
            when {
                detail != null -> {
                    detail.viewCount.takeIf { it > 0 }?.let { InfoStatItem(label = "浏览", value = it) }
                    detail.discussCount.takeIf { it > 0 }?.let { InfoStatItem(label = "讨论", value = it) }
                }
                detailErrorMessage != null -> TextButton(onClick = onRetryDetail) {
                    Text(
                        "信息加载失败，点击重试",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                else -> Text(
                    "正在加载话题信息…",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        actions = {
            FollowButton(
                isFollowing = detail?.isFollowing == true,
                onClick = { detail?.let { onFollowingChange(!it.isFollowing) } },
                isLoading = isFollowingChanging,
                modifier = Modifier.weight(1f),
            )
        },
        introduction = {
            detail?.excerpt?.takeIf(String::isNotBlank)?.let { introduction ->
                ExpandableContent(
                    isExpanded = isIntroductionExpanded,
                    onExpandedChange = onExpandedChange,
                    collapsedMaxHeight = collapsedTextHeight(10, MaterialTheme.typography.bodyMedium),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = introduction,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        overflow = TextOverflow.Clip,
                    )
                }
            }
        },
    )
}
