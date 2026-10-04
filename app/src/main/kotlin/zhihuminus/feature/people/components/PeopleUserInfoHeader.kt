package com.zhihuminus.feature.people.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.zhihuminus.feature.people.PeopleProfile
import com.zhihuminus.ui.components.AuthorBadge

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PeopleUserInfoHeader(
    profile: PeopleProfile,
    onFollowToggle: () -> Unit,
    onBlockToggle: () -> Unit,
    onStatClick: (Int) -> Unit,
    onAvatarClick: (String) -> Unit,
    onExternalUrlClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = profile.avatarUrl,
                contentDescription = "用户头像",
                modifier = Modifier
                    .padding(end = 16.dp)
                    .size(80.dp)
                    .clip(CircleShape)
                    .clickable {
                        onAvatarClick(profile.avatarUrl.substringBefore("_") + ".jpg")
                    },
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    profile.officialBadge?.let { badge ->
                        AuthorBadge(
                            badge = badge,
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }
                if (profile.headline.isNotEmpty()) {
                    Text(
                        text = profile.headline,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                PeopleOfficialBadgeDetails(
                    badges = profile.officialBadgeDetails,
                    modifier = Modifier.padding(top = 6.dp),
                )
                profile.githubSocial?.let { githubSocial ->
                    Row(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .clickable { onExternalUrlClick(githubSocial.profileUrl) },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        githubSocial.iconUrl?.let { iconUrl ->
                            AsyncImage(
                                model = iconUrl,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        Text(
                            text = githubSocial.title,
                            modifier = Modifier.weight(1f, fill = false),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "· ${githubSocial.starCount} stars",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            PeopleStatItem("回答", profile.answerCount, onClick = { onStatClick(0) })
            PeopleStatItem("文章", profile.articleCount, onClick = { onStatClick(1) })
            PeopleStatItem("粉丝", profile.followerCount, onClick = { onStatClick(7) })
            PeopleStatItem("关注", profile.followingCount, onClick = { onStatClick(8) })
        }
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(onClick = onFollowToggle) {
                Text(if (profile.isFollowing) "取消关注" else "关注")
            }
            OutlinedButton(onClick = onBlockToggle) {
                Text(if (profile.isBlocking) "取消拉黑" else "拉黑")
            }
        }
    }
}
