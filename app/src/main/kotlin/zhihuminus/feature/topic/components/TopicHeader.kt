package com.zhihuminus.feature.topic.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.zhihuminus.core.util.formatCount
import com.zhihuminus.feature.topic.TopicDetail

@Composable
fun TopicHeader(
    detail: TopicDetail?,
    detailErrorMessage: String?,
    topicName: String,
    isFollowingChanging: Boolean,
    onRetryDetail: () -> Unit,
    onFollowingChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            AsyncImage(
                model = detail?.avatarUrl,
                contentDescription = "话题头像",
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape),
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    detail?.name?.ifBlank { topicName } ?: topicName,
                    style = MaterialTheme.typography.headlineSmall,
                )
                when {
                    detail != null -> Text(
                        listOfNotNull(
                            detail.viewCount.takeIf { it > 0 }?.let { "${formatCount(it)}浏览" },
                            detail.discussCount.takeIf { it > 0 }?.let { "${formatCount(it)}讨论" },
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.labelMedium,
                    )

                    detailErrorMessage != null -> TextButton(onClick = onRetryDetail) {
                        Text("话题信息加载失败：$detailErrorMessage，点击重试", color = MaterialTheme.colorScheme.error)
                    }

                    else -> Text("正在加载话题信息…")
                }
            }
            Button(
                onClick = { detail?.let { onFollowingChange(!it.isFollowing) } },
                enabled = detail != null && !isFollowingChanging,
                modifier = Modifier.semantics { selected = detail?.isFollowing == true },
            ) {
                if (isFollowingChanging) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(if (detail?.isFollowing == true) "已关注" else "关注话题", maxLines = 1)
                }
            }
        }
    }
}
