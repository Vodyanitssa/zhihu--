package com.zhihuminus.feature.notification.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.zhihuminus.core.content.AstParser
import com.zhihuminus.core.content.renderer.InlineNodes
import com.zhihuminus.feature.notification.NotificationTimelineItem
import com.zhihuminus.util.formatRelativeTime

@Composable
fun NotificationItemView(
    notification: NotificationTimelineItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = if (notification.isRead) {
        Color.Transparent
    } else {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = backgroundColor,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier = Modifier
                        .width(10.dp)
                        .padding(top = 10.dp, end = 4.dp),
                ) {
                    if (!notification.isRead) {
                        Surface(
                            color = MaterialTheme.colorScheme.error,
                            shape = CircleShape,
                            modifier = Modifier.size(6.dp),
                        ) {}
                    }
                }
                val avatarUrl = notification.avatarUrl()
                if (avatarUrl.isNotBlank()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = notification.displayTitle(),
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = formatRelativeTime(notification.created),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    notification.content?.subTitle?.takeIf { it.isNotBlank() }?.let { subtitle ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$subtitle：",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    val contentText = when {
                        !notification.content?.abstractText.isNullOrBlank() -> notification.content.abstractText
                        notification.content?.subTitle == "喜欢了你的评论" -> notification.content.subText
                        else -> notification.content?.text.orEmpty()
                    }

                    val inlineNodes = remember(contentText) {
                        AstParser.parseInline(contentText)
                    }
                    if (inlineNodes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        InlineNodes(
                            nodes = inlineNodes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    val sourceTitle = notification.targetSource?.text?.takeIf { it.isNotBlank() }
                    val sourceExcerpt = notification.targetSource?.subText?.takeIf { it.isNotBlank() }
                    if (sourceTitle != null || sourceExcerpt != null) {
                        val indicatorColor = MaterialTheme.colorScheme.outlineVariant
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .drawBehind {
                                    drawRoundRect(
                                        color = indicatorColor,
                                        topLeft = Offset.Zero,
                                        size = Size(
                                            width = 3.dp.toPx(),
                                            height = size.height,
                                        ),
                                        cornerRadius = CornerRadius(1.5.dp.toPx()),
                                    )
                                }.padding(start = 10.dp),
                        ) {
                            if (sourceTitle != null) {
                                Text(
                                    text = sourceTitle,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            if (sourceExcerpt != null) {
                                if (sourceTitle != null) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                }
                                Text(
                                    text = sourceExcerpt,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
