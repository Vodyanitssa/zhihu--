package com.zhihuminus.feature.search.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.zhihuminus.core.util.formatCount
import com.zhihuminus.feature.search.TopicSearchResult

@Composable
fun SearchTopicItem(
    result: TopicSearchResult,
    isChangingFollowing: Boolean,
    onClick: () -> Unit,
    onToggleFollowing: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val topic = result.topic
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = topic.avatarUrl,
            contentDescription = "${topic.name}的图标",
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp)),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(
                text = topic.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (result.excerpt.isNotEmpty()) {
                Text(
                    text = result.excerpt,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = "${formatCount(result.visitCount)} 浏览 · ${formatCount(result.discussCount)} 讨论",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(
            enabled = !isChangingFollowing,
            onClick = { onToggleFollowing(!result.isFollowing) },
        ) {
            Text(if (result.isFollowing) "已关注" else "关注")
        }
    }
}
