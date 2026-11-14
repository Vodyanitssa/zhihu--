package com.zhihuminus.feature.collection.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zhihuminus.core.error.AppError
import com.zhihuminus.core.util.formatCount
import com.zhihuminus.core.util.formatRelativeTime
import com.zhihuminus.feature.collection.Collection
import com.zhihuminus.ui.components.ExpandableContent
import com.zhihuminus.ui.components.collapsedTextHeight

@Composable
fun CollectionHeader(
    collection: Collection?,
    detailErrorMessage: AppError?,
    isDescriptionExpanded: Boolean,
    onDescriptionExpandedChange: (Boolean) -> Unit,
    onRetryDetail: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (collection != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = collection.title.takeIf { it.isNotBlank() } ?: "收藏夹",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(0.5f),
                )
                Spacer(Modifier.width(12.dp))
                CollectionStatsBar(
                    collection = collection,
                    modifier = Modifier.weight(0.5f),
                )
            }

            val updatedText = remember(collection.updatedTime) { formatRelativeTime(collection.updatedTime) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = if (collection.isPublic) "公开" else "仅自己可见",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (collection.updatedTime > 0) {
                    Text(
                        text = "·",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                    Text(
                        text = "更新于$updatedText",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            when {
                detailErrorMessage != null -> TextButton(onClick = onRetryDetail) {
                    Text(
                        text = "收藏夹信息加载失败: ${detailErrorMessage.userMessage}，点击重试",
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                else -> Text(
                    text = "正在加载收藏夹信息…",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        val description = collection?.description?.takeIf { it.isNotBlank() }
        if (description != null) {
            ExpandableContent(
                isExpanded = isDescriptionExpanded,
                onExpandedChange = onDescriptionExpandedChange,
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
private fun CollectionStatsBar(
    collection: Collection,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Top,
    ) {
        CollectionStatItem(label = "关注", value = collection.followerCount)
        CollectionStatItem(label = "浏览", value = collection.viewCount)
        CollectionStatItem(label = "赞同", value = collection.likeCount)
    }
}

@Composable
private fun CollectionStatItem(
    label: String,
    value: Int,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            text = formatCount(value),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
