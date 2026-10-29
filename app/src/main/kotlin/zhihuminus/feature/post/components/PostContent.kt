package com.zhihuminus.feature.post.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zhihuminus.core.content.ContentNode
import com.zhihuminus.core.content.renderer.LocalImageViewManager
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
    val imageViewerManager = LocalImageViewManager.current
    val isPin = post.type == PostType.Pin
    val pinImages = remember(post.content, isPin) {
        if (isPin) post.content.filterIsInstance<ContentNode.Image>() else emptyList()
    }
    val pinTextNodes = remember(post.content, isPin) {
        if (isPin) post.content.filterNot { it is ContentNode.Image } else post.content
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 72.dp),
    ) {
        SelectionContainer {
            RenderContentNodes(if (isPin) pinTextNodes else post.content)
        }

        // Pin image gallery
        if (pinImages.isNotEmpty()) {
            PinImageGallery(
                images = pinImages,
                onImageClick = { url -> imageViewerManager?.show(url) },
            )
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
            Column(modifier = Modifier.padding(top = 16.dp)) {
                Text(
                    text = "话题",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                )
                post.topics.forEach { topic ->
                    Text(
                        text = "#${topic.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { inAppLinkOpener("https://www.zhihu.com/topic/${topic.id}") }
                            .padding(vertical = 4.dp),
                    )
                }
            }
        }
    }
}
