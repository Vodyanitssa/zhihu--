package com.zhihuminus.feature.post.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zhihuminus.feature.post.PostLinkCard
import com.zhihuminus.feature.post.PostType
import com.zhihuminus.navigation.NavDestination
import com.zhihuminus.navigation.PostDestination
import com.zhihuminus.navigation.Question
import com.zhihuminus.navigation.resolveContent

@Composable
fun PostLinkCard(
    linkCard: PostLinkCard,
    onClick: () -> Unit = {},
) {
    Spacer(modifier = Modifier.height(16.dp))
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "关联内容",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = linkCardTypeLabel(linkCard.dataContentType),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (linkCard.url.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = linkCard.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun linkCardTypeLabel(dataContentType: String): String = when (dataContentType.lowercase()) {
    "answer" -> "回答"
    "article" -> "文章"
    "question" -> "问题"
    "pin" -> "想法"
    "people" -> "用户"
    "video", "zvideo" -> "视频"
    else -> dataContentType
}

fun resolveLinkCardDestination(linkCard: PostLinkCard): NavDestination? {
    val byUrl = linkCard.url
        .takeIf { it.isNotBlank() }
        ?.let(::resolveContent)
    if (byUrl != null) return byUrl

    val contentId = linkCard.dataContentId
    return when (linkCard.dataContentType.lowercase()) {
        "answer" -> contentId.toLongOrNull()?.let { PostDestination(type = PostType.Answer, id = it) }
        "article" -> contentId.toLongOrNull()?.let { PostDestination(type = PostType.Article, id = it) }
        "pin" -> contentId.toLongOrNull()?.let { PostDestination(type = PostType.Pin, id = it) }
        "question" -> contentId.toLongOrNull()?.let { Question(questionId = it) }
        else -> null
    }
}
