package com.zhihuminus.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.zhihuminus.core.util.formatCount
import com.zhihuminus.data.DataHolder
import com.zhihuminus.data.officialBadge
import com.zhihuminus.feature.people.PeopleMemberItem

@Composable
fun PeopleListItem(
    people: PeopleMemberItem,
    onClick: () -> Unit,
    onToggleFollow: () -> Unit,
    modifier: Modifier = Modifier,
    highlightedName: String? = null,
    isFollowing: Boolean = people.isFollowing,
    isChangingFollowing: Boolean = false,
    showBadge: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = people.avatarUrl,
            contentDescription = "${people.name}的头像",
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (!highlightedName.isNullOrEmpty()) {
                        searchHighlightedText(highlightedName)
                    } else {
                        AnnotatedString(people.name)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (showBadge) {
                    val officialBadge = people.officialBadge
                    if (officialBadge?.isUsefulInList == true) {
                        AuthorBadge(
                            badge = officialBadge,
                            compact = true,
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }
            }
            if (people.headline.isNotBlank()) {
                Text(
                    text = people.headline,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 2.dp),
            ) {
                Text(
                    text = "${formatCount(people.answerCount.toLong())} 回答",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${formatCount(people.articleCount.toLong())} 文章",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${formatCount(people.followerCount.toLong())} 粉丝",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        OutlinedButton(
            onClick = onToggleFollow,
            enabled = !isChangingFollowing,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
        ) {
            Text(if (isFollowing) "取消关注" else "关注")
        }
    }
}

@Deprecated("Use PeopleMemberItem overload instead")
@Composable
fun PeopleListItem(
    people: DataHolder.People,
    onClick: () -> Unit,
    onToggleFollow: () -> Unit,
    modifier: Modifier = Modifier,
    highlightedName: String? = null,
    isFollowing: Boolean = people.isFollowing,
    isChangingFollowing: Boolean = false,
    showBadge: Boolean = true,
) {
    PeopleListItem(
        people = PeopleMemberItem(
            id = people.id,
            urlToken = people.urlToken ?: "",
            name = people.name,
            avatarUrl = people.avatarUrl,
            headline = people.headline,
            officialBadge = people.badgeV2.officialBadge(),
            answerCount = people.answerCount,
            articleCount = people.articlesCount,
            followerCount = people.followerCount,
            isFollowing = isFollowing,
        ),
        onClick = onClick,
        onToggleFollow = onToggleFollow,
        modifier = modifier,
        highlightedName = highlightedName,
        isFollowing = isFollowing,
        isChangingFollowing = isChangingFollowing,
        showBadge = showBadge,
    )
}
