package com.zhihuminus.feature.notification.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.zhihuminus.core.content.AstParser
import com.zhihuminus.core.content.renderer.InlineNodes
import com.zhihuminus.core.util.formatRelativeTime
import com.zhihuminus.feature.notification.NotificationTimelineItem

/**
 * 知乎活动助手通知卡片。
 *
 * 数据来自 `noti_activity_card`：
 * - `content.title` 是账号名（知乎游戏/知乎科学等）
 * - `content.subTitle` 是活动标题
 * - `content.text` 是活动正文（可能带 HTML）
 * - `content.subText` 是活动时间
 * - `content.abstractText` 是按钮文案
 * - `targetSource.imageDisplayInfo.displayImage.src` 是封面大图
 */
@Composable
fun ActivityNotificationCard(
    notification: NotificationTimelineItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val timeText = remember(notification.created, notification.createdStr) {
        notification.created
            .takeIf { it > 0 }
            ?.let(::formatRelativeTime)
            .orEmpty()
            .ifBlank { notification.createdStr }
    }
    val descriptionNodes = remember(notification) {
        AstParser.parseInline(notification.activityDescription())
    }
    val coverUrl = notification.activityCoverUrl()
    val cardShape = RoundedCornerShape(16.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!notification.isRead) {
                Surface(
                    color = MaterialTheme.colorScheme.error,
                    shape = CircleShape,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(6.dp),
                ) {}
            }
            val avatarUrl = notification.avatarUrl()
            if (avatarUrl.isNotBlank()) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
                Spacer(Modifier.width(10.dp))
            }
            Text(
                text = notification.activitySourceName(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (timeText.isNotBlank()) {
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            shape = cardShape,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column {
                if (coverUrl != null) {
                    AsyncImage(
                        model = coverUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(2.45f)
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                    )
                }
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = notification.activityTitle(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (descriptionNodes.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        InlineNodes(
                            nodes = descriptionNodes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            applyTypographyConfig = false,
                        )
                    }
                    val activityTime = notification.activityTimeText()
                    if (activityTime.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = activityTime,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = onClick,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(notification.activityActionText())
                        }
                    }
                }
            }
        }
    }
}
