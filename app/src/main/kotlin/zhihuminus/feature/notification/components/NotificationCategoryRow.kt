package com.zhihuminus.feature.notification.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PersonAddAlt1
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zhihuminus.feature.notification.NotificationCategory

@Composable
fun NotificationCategoryRow(
    unreadCounts: Map<NotificationCategory, Int>,
    showUnreadBadges: Boolean,
    onCategoryClick: (NotificationCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        NotificationCategory.entries.forEach { category ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onCategoryClick(category) }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BadgedBox(
                    badge = {
                        val unreadCount = unreadCounts[category] ?: 0
                        if (showUnreadBadges && unreadCount > 0) {
                            Badge {
                                Text(formatUnreadCount(unreadCount))
                            }
                        }
                    },
                ) {
                    Icon(
                        imageVector = category.icon(),
                        contentDescription = category.detailTitle,
                        modifier = Modifier.size(36.dp),
                    )
                }
                Text(
                    text = category.detailTitle,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun NotificationCategory.icon(): ImageVector = when (this) {
    NotificationCategory.Comment -> Icons.AutoMirrored.Outlined.Comment
    NotificationCategory.Like -> Icons.Filled.Favorite
    NotificationCategory.Favorite -> Icons.Filled.Bookmark
    NotificationCategory.Follow -> Icons.Filled.PersonAddAlt1
}

private fun formatUnreadCount(count: Int): String = if (count > 99) "99+" else count.toString()
