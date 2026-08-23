package com.zhihuminus.feature.post.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zhihuminus.core.content.renderer.ContentNodes
import com.zhihuminus.feature.post.Post
import com.zhihuminus.feature.post.PostEvent
import com.zhihuminus.feature.post.PostType

@Composable
fun PostContent(
    post: Post,
    onEvent: (PostEvent) -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 72.dp),
    ) {
        // Title (for articles and answers)
        if (post.type != PostType.Pin && post.title.isNotBlank()) {
            Text(
                text = post.title,
                style = when (post.type) {
                    PostType.Article -> MaterialTheme.typography.headlineSmall
                    PostType.Answer -> MaterialTheme.typography.titleMedium
                    PostType.Pin -> MaterialTheme.typography.bodyMedium // unreachable
                },
                fontWeight = FontWeight.Bold,
                color = when (post.type) {
                    PostType.Article -> MaterialTheme.colorScheme.onSurface
                    PostType.Answer -> MaterialTheme.colorScheme.onSurfaceVariant
                    PostType.Pin -> MaterialTheme.colorScheme.onSurface // unreachable
                },
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }

        // Content nodes
        ContentNodes(
            nodes = post.content,
            modifier = Modifier.fillMaxWidth(),
        )

        // Poll card (Pin only)
        val poll = post.poll
        if (post.type == PostType.Pin && poll != null) {
            PostPollCard(
                poll = poll,
                onPollVote = { pollId, optionId ->
                    onEvent(PostEvent.VotePoll(pollId, optionId))
                },
            )
        }

        // Link cards (Pin only)
        if (post.type == PostType.Pin && post.linkCards.isNotEmpty()) {
            for (linkCard in post.linkCards) {
                PostLinkCard(
                    linkCard = linkCard,
                    onClick = {
                        val destination = resolveLinkCardDestination(linkCard)
                        if (destination != null) {
                            onEvent(PostEvent.Navigate(destination))
                        } else if (linkCard.url.isNotBlank()) {
                            onEvent(PostEvent.OpenLink(linkCard.url))
                        }
                    },
                )
            }
        }

        // Topics (for articles and pins)
        if (post.topics.isNotEmpty()) {
            Text(
                text = post.topics.joinToString(" · ") { "#$it" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}
