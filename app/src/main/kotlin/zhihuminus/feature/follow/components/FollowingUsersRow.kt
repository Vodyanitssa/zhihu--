package com.zhihuminus.feature.follow.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.zhihuminus.feature.follow.FollowingUser

/** 已关注人条目的自然高度（8dp 上下 contentPadding + 4dp 条目 padding + 56dp 头像 + 4dp 间隔 + 18dp 名字）。 */
val FollowingUsersRowHeight = 102.dp

@Composable
fun FollowingUsersRow(
    users: List<FollowingUser>,
    errorMessage: String?,
    onUserClick: (FollowingUser) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 固定高度占位：首帧即占据最终尺寸，避免数据到达后首个列表项变高导致 LazyColumn 滚动锚定把整行顶出视口。
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(FollowingUsersRowHeight),
    ) {
        when {
            errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            users.isNotEmpty() -> {
                LazyRow(
                    modifier = Modifier,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(users, key = { it.id }) { user ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { onUserClick(user) }
                                .padding(vertical = 4.dp),
                        ) {
                            BadgedBox(
                                badge = {
                                    if (user.unreadCount > 0) {
                                        Badge()
                                    }
                                },
                            ) {
                                AsyncImage(
                                    model = user.avatarUrl,
                                    contentDescription = user.name,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape),
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = user.name,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.size(width = 60.dp, height = 18.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
