package com.zhihuminus.feature.post.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.zhihuminus.feature.post.Author
import com.zhihuminus.util.formatCompactCount
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

@Composable
fun PostHeader(
    author: Author,
    createdAt: Long,
    updatedAt: Long,
    ipInfo: String?,
    voteCount: Int = 0,
    firstVoterName: String? = null,
    isFollowing: Boolean = false,
    onShowVoters: () -> Unit = {},
    onFollowClick: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = author.avatarUrl,
            contentDescription = author.name,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = author.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            if (author.headline.isNotBlank()) {
                Text(
                    text = author.headline,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        // Follow button
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isFollowing) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.primaryContainer
            },
            modifier = Modifier.clickable(onClick = onFollowClick),
        ) {
            Text(
                text = if (isFollowing) "已关注" else "关注",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isFollowing) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                },
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }
    }
    // Time and IP info
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 8.dp),
    ) {
        val timeText = formatTimeText(createdAt, updatedAt)
        val infoText = buildString {
            if (timeText.isNotBlank()) append(timeText)
            if (!ipInfo.isNullOrBlank()) {
                if (isNotEmpty()) append(" · ")
                append(ipInfo)
            }
        }
        if (infoText.isNotBlank()) {
            Text(
                text = infoText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // Social proof
        if (voteCount > 0) {
            Spacer(modifier = Modifier.height(4.dp))
            val socialProofText = if (firstVoterName != null) {
                "$firstVoterName 等 ${formatCompactCount(voteCount)} 人赞同"
            } else {
                "${formatCompactCount(voteCount)} 人赞同"
            }
            Text(
                text = socialProofText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable(onClick = onShowVoters),
            )
        }
    }
}

private fun formatTimeText(createdAt: Long, updatedAt: Long): String {
    if (createdAt <= 0) return ""
    val createdStr = Instant
        .ofEpochSecond(createdAt)
        .atZone(ZoneId.systemDefault())
        .format(dateFormatter)
    if (updatedAt <= 0 || updatedAt == createdAt) {
        return "发布于 $createdStr"
    }
    val updatedStr = Instant
        .ofEpochSecond(updatedAt)
        .atZone(ZoneId.systemDefault())
        .format(dateFormatter)
    return "发布于 $createdStr · 编辑于 $updatedStr"
}
