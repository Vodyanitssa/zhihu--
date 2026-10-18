package com.zhihuminus.feature.people.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.zhihuminus.core.content.AstParser
import com.zhihuminus.core.content.renderer.InlineNodes
import com.zhihuminus.feature.people.PeopleProfile
import com.zhihuminus.ui.components.AuthorBadge
import org.jsoup.Jsoup

@Composable
fun PeopleUserInfoHeader(
    profile: PeopleProfile,
    onFollowToggle: () -> Unit,
    onFollowingClick: () -> Unit,
    onFollowersClick: () -> Unit,
    onAvatarClick: (String) -> Unit,
    onExternalUrlClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val rightWidth = maxWidth * 0.6f
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                AsyncImage(
                    model = profile.avatarUrl,
                    contentDescription = "用户头像",
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .clickable {
                            onAvatarClick(profile.avatarUrl.substringBefore("_") + ".jpg")
                        },
                )

                Column(
                    modifier = Modifier.width(rightWidth),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PeopleStatItem(
                            label = "关注",
                            value = profile.followingCount,
                            onClick = onFollowingClick,
                        )
                        PeopleStatItem(
                            label = "粉丝",
                            value = profile.followerCount,
                            onClick = onFollowersClick,
                        )
                        if (profile.voteupCount > 0) {
                            PeopleStatItem(
                                label = "获赞",
                                value = profile.voteupCount,
                            )
                        }
                    }

                    val buttonModifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .height(36.dp)

                    if (profile.isFollowing) {
                        OutlinedButton(
                            onClick = onFollowToggle,
                            modifier = buttonModifier,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        ) {
                            Text(if (profile.isFollowed) "互相关注" else "已关注")
                        }
                    } else {
                        Button(
                            onClick = onFollowToggle,
                            modifier = buttonModifier,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        ) {
                            Text(if (profile.isFollowed) "回关" else "+ 关注")
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = profile.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
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
            if (!profile.isFollowing && profile.isFollowed) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(start = 6.dp),
                ) {
                    Text(
                        text = "关注了你",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                }
            }
            if (profile.ipInfo.isNotBlank()) {
                Text(
                    text = if (profile.ipInfo.startsWith("IP 属地")) profile.ipInfo else "IP 属地：${profile.ipInfo}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }

        val headlineNodes = remember(profile.headline) {
            if (profile.headline.isBlank()) {
                emptyList()
            } else {
                AstParser.parseInline(profile.headline.trim())
            }
        }

        if (headlineNodes.isNotEmpty()) {
            InlineNodes(
                nodes = headlineNodes,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        val descriptionNodes = remember(profile.description, profile.headline) {
            if (profile.description.isBlank()) return@remember emptyList()
            val plainDesc = Jsoup.parse(profile.description).text().trim()
            val plainHeadline = Jsoup.parse(profile.headline).text().trim()
            if (plainDesc.isBlank() || plainDesc == profile.headline.trim() || plainDesc == plainHeadline) {
                emptyList()
            } else {
                AstParser.parseInline(profile.description.trim())
            }
        }

        if (descriptionNodes.isNotEmpty()) {
            InlineNodes(
                nodes = descriptionNodes,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        PeopleOfficialBadgeDetails(
            badges = profile.officialBadgeDetails,
            modifier = Modifier.padding(top = 6.dp),
        )

        profile.githubSocial?.let { githubSocial ->
            Row(
                modifier = Modifier
                    .padding(top = 6.dp)
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
