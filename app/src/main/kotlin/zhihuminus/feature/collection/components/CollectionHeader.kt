package com.zhihuminus.feature.collection.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zhihuminus.core.error.AppError
import com.zhihuminus.core.util.formatRelativeTime
import com.zhihuminus.feature.collection.Collection
import com.zhihuminus.ui.components.ExpandableContent
import com.zhihuminus.ui.components.InfoHeader
import com.zhihuminus.ui.components.InfoStatItem
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
    InfoHeader(
        modifier = modifier,
        iconUrl = null,
        iconShape = RoundedCornerShape(12.dp),
        iconBackgroundColor = MaterialTheme.colorScheme.primaryContainer,
        iconContent = {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Bookmarks,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(40.dp),
                )
            }
        },
        title = collection?.title?.takeIf { it.isNotBlank() } ?: "收藏夹",
        stats = {
            when {
                collection != null -> {
                    InfoStatItem(label = "关注", value = collection.followerCount.toLong())
                    InfoStatItem(label = "浏览", value = collection.viewCount.toLong())
                    InfoStatItem(label = "赞同", value = collection.likeCount.toLong())
                }
                detailErrorMessage != null -> TextButton(onClick = onRetryDetail) {
                    Text(
                        "信息加载失败，点击重试",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                else -> Text(
                    "正在加载收藏夹信息…",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        introduction = {
            collection?.description?.takeIf { it.isNotBlank() }?.let { description ->
                ExpandableContent(
                    isExpanded = isDescriptionExpanded,
                    onExpandedChange = onDescriptionExpandedChange,
                    collapsedMaxHeight = collapsedTextHeight(10, MaterialTheme.typography.bodyMedium),
                    modifier = Modifier.fillMaxWidth(),
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
            collection?.let { loadedCollection ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = if (loadedCollection.isPublic) "公开" else "仅自己可见",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (loadedCollection.updatedTime > 0) {
                        val updatedText = remember(loadedCollection.updatedTime) {
                            formatRelativeTime(loadedCollection.updatedTime)
                        }
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
            }
        },
    )
}
