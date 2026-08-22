package com.zhihuminus.feature.post.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.zhihuminus.feature.post.Author
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
            Text(
                text = author.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
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
