package com.zhihuminus.feature.column.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.zhihuminus.core.error.AppError
import com.zhihuminus.core.util.formatCount
import com.zhihuminus.core.util.formatRelativeTime
import com.zhihuminus.feature.column.ColumnDetail
import com.zhihuminus.ui.components.ExpandableContent
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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            AsyncImage(
                model = detail?.avatarUrl,
                contentDescription = "专栏头像",
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            Spacer(Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = detail?.title?.takeIf { it.isNotBlank() } ?: "专栏",
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                when {
                    detail != null -> ColumnStats(detail)
                    detailErrorMessage != null -> TextButton(onClick = onRetryDetail) {
                        Text(
                            "专栏信息加载失败: ${detailErrorMessage.userMessage}，点击重试",
                            color = MaterialTheme.colorScheme.error,
                        )
                    }

                    else -> Text(
                        "正在加载专栏信息…",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (detail != null) {
                Button(
                    onClick = { onFollowingChange(!detail.isFollowing) },
                    enabled = !isFollowingChanging,
                    modifier = Modifier.semantics { selected = detail.isFollowing },
                ) {
                    if (isFollowingChanging) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text(if (detail.isFollowing) "已关注" else "关注专栏", maxLines = 1)
                    }
                }
            }
        }

        detail?.description?.takeIf { it.isNotBlank() }?.let { description ->
            ExpandableContent(
                isExpanded = isIntroductionExpanded,
                onExpandedChange = onExpandedChange,
                collapsedMaxHeight = collapsedTextHeight(10, MaterialTheme.typography.bodyMedium),
            ) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun ColumnStats(detail: ColumnDetail) {
    val stats = listOfNotNull(
        detail.articleCount.takeIf { it > 0 }?.let { "${formatCount(it)} 篇文章" },
        detail.followerCount.takeIf { it > 0 }?.let { "${formatCount(it)} 人关注" },
        detail.voteupCount.takeIf { it > 0 }?.let { "${formatCount(it)} 赞" },
    )
    val createdText = remember(detail.createdAt) { formatRelativeTime(detail.createdAt) }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (stats.isNotEmpty()) {
            Text(
                text = stats.joinToString(" · "),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (createdText.isNotBlank()) {
            Text(
                text = "创建于$createdText",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
