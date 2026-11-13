package com.zhihuminus.feature.question.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zhihuminus.core.content.ContentNode
import com.zhihuminus.core.content.renderer.RenderContentNodes
import com.zhihuminus.core.util.formatCount
import com.zhihuminus.feature.question.QuestionDetail
import com.zhihuminus.feature.question.QuestionEvent
import com.zhihuminus.feature.question.QuestionSort
import com.zhihuminus.feature.question.QuestionTopic
import com.zhihuminus.navigation.Topic
import com.zhihuminus.ui.components.ExpandableContent
import com.zhihuminus.ui.components.FollowButton
import com.zhihuminus.ui.components.FollowButtonHeight
import com.zhihuminus.ui.components.TopicTag
import com.zhihuminus.ui.components.TopicTagList

private val QUESTION_DETAIL_COLLAPSED_MAX_HEIGHT: Dp = 180.dp

/**
 * 问题标题与统计信息。
 */
@Composable
internal fun QuestionHeaderSection(
    title: String,
    visitCount: Int,
    commentCount: Int,
    followerCount: Int,
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SelectionContainer {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatItem(icon = Icons.Outlined.Visibility, text = "${formatCount(visitCount)} 浏览")
            StatItem(icon = Icons.Outlined.ChatBubbleOutline, text = "${formatCount(commentCount)} 评论")
            StatItem(icon = Icons.Outlined.FavoriteBorder, text = "${formatCount(followerCount)} 关注")
        }
    }
}

/**
 * 问题详情（可折叠）+ 主操作（评论/关注）+ 回答排序切换。
 */
@Composable
internal fun QuestionBodyHeader(
    detail: QuestionDetail,
    contentNodes: List<ContentNode>,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onShowComments: () -> Unit,
    currentSort: QuestionSort,
    onEvent: (QuestionEvent) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ExpandableContent(
            isExpanded = isExpanded,
            onExpandedChange = onExpandedChange,
            collapsedMaxHeight = QUESTION_DETAIL_COLLAPSED_MAX_HEIGHT,
            modifier = Modifier.fillMaxWidth(),
        ) {
            QuestionDetailWithTopics(
                contentNodes = contentNodes,
                topics = detail.topics,
                onTopicClick = { topic -> onEvent(QuestionEvent.Navigate(Topic(id = topic.id, section = topic.name))) },
            )
        }
        QuestionPrimaryActions(
            isFollowing = detail.isFollowing,
            onFollowClick = { onEvent(QuestionEvent.ToggleFollow) },
            onShowComments = onShowComments,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${formatCount(detail.answerCount)} 回答",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.weight(1f))
            SortChip(label = "默认", selected = currentSort == QuestionSort.DEFAULT) {
                onEvent(QuestionEvent.ChangeSort(QuestionSort.DEFAULT))
            }
            SortChip(label = "最新", selected = currentSort == QuestionSort.LATEST) {
                onEvent(QuestionEvent.ChangeSort(QuestionSort.LATEST))
            }
        }
    }
}

@Composable
private fun SortChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = Modifier.semantics { this.selected = selected },
        label = { Text(label) },
    )
}

@Composable
private fun QuestionDetailWithTopics(
    contentNodes: List<ContentNode>,
    topics: List<QuestionTopic>,
    onTopicClick: (TopicTag) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (contentNodes.isNotEmpty()) {
            RenderContentNodes(contentNodes)
        }
        if (topics.isNotEmpty()) {
            TopicTagList(
                topics = topics.map { TopicTag(id = it.id, name = it.name) },
                onTopicClick = onTopicClick,
            )
        }
    }
}

@Composable
private fun QuestionPrimaryActions(
    isFollowing: Boolean,
    onFollowClick: () -> Unit,
    onShowComments: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(FollowButtonHeight),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedButton(
            onClick = onShowComments,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
        ) {
            Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("评论")
        }
        FollowButton(
            isFollowing = isFollowing,
            onClick = onFollowClick,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatItem(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
