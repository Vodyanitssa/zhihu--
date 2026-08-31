package com.zhihuminus.feature.post.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zhihuminus.core.content.ContentNode
import com.zhihuminus.core.content.renderer.RenderContentNode
import com.zhihuminus.core.content.renderer.RenderContentNodes
import com.zhihuminus.feature.post.Post
import com.zhihuminus.feature.post.PostEvent
import com.zhihuminus.feature.post.PostType
import com.zhihuminus.navigation.link.rememberInAppLinkOpener

@Composable
fun PostContent(
    post: Post,
    onEvent: (PostEvent) -> Unit = {},
) {
    val inAppLinkOpener = rememberInAppLinkOpener()
    val pinImages = mutableListOf<ContentNode>()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 72.dp),
    ) {
        if (post.type == PostType.Pin) {
            post.content.forEach { contentNode ->
                when (contentNode) {
                    is ContentNode.Image -> {
                        pinImages += contentNode
                    }

                    else -> RenderContentNode(contentNode)
                }
            }
        } else {
            SelectionContainer {
                RenderContentNodes(post.content)
            }
        }

        // Pin image gallery
        if (pinImages.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .height(400.dp),
            ) {
                pinImages.forEach { image ->
                    RenderContentNode(image)
                }
            }
        }

        // Poll card (Pin only)
        val poll = post.poll
        if (poll != null) {
            PostPollCard(
                poll = poll,
                onPollVote = { pollId, optionId ->
                    onEvent(PostEvent.VotePoll(pollId, optionId))
                },
            )
        }

        // Link cards (Pin only)
        if (post.linkCards.isNotEmpty()) {
            for (linkCard in post.linkCards) {
                PostLinkCard(
                    linkCard = linkCard,
                    onClick = {
                        inAppLinkOpener(linkCard.url)
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
