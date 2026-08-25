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
import com.zhihuminus.core.util.formatDateTime
import com.zhihuminus.feature.post.Author
import com.zhihuminus.navigation.LocalNavigator
import com.zhihuminus.navigation.Person
import com.zhihuminus.navigation.Question
import com.zhihuminus.util.formatCompactCount

@Composable
fun PostHeader(
    title: String?,
    author: Author,
    createdAt: Long,
    updatedAt: Long,
    ipInfo: String?,
    voteCount: Int = 0,
    isFollowing: Boolean = false,
    questionId: Long? = null,
    onFollowClick: () -> Unit = {},
) {
    val navigator = LocalNavigator.current
    if (title != null) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                modifier = if (questionId != null) {
                    Modifier.clickable {
                        navigator.onNavigate(Question(questionId = questionId, title = title))
                    }
                } else {
                    Modifier
                },
            )
        }
    }
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
                .clip(CircleShape)
                .then(
                    if (author.id.isNotBlank() || author.urlToken.isNotBlank()) {
                        Modifier.clickable {
                            navigator.onNavigate(
                                Person(id = author.id, urlToken = author.urlToken, name = author.name),
                            )
                        }
                    } else {
                        Modifier
                    },
                ),
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
        val createdTime = formatDateTime(createdAt)
        var timeText = "发布于 $createdTime"
        if (updatedAt > 0 && updatedAt != createdAt) {
            val updatedTime = formatDateTime(updatedAt)
            timeText += " · 编辑于 $updatedTime"
        }
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
            Text(
                text = "${formatCompactCount(voteCount)} 人赞同",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
