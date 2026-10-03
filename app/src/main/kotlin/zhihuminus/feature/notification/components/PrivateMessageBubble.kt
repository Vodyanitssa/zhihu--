package com.zhihuminus.feature.notification.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.zhihuminus.core.util.formatDateTime
import com.zhihuminus.feature.notification.PrivateMessage
import org.jsoup.Jsoup

@Composable
fun PrivateMessageBubble(
    message: PrivateMessage,
    incoming: Boolean,
    modifier: Modifier = Modifier,
    showTimestamp: Boolean = true,
) {
    val displayText = (
        message.plugin?.excerpt?.takeIf { it.isNotBlank() }
            ?: message.content.takeIf { it.isNotBlank() }
    )?.let { Jsoup.parse(it).text() }
        ?: "暂不支持显示这条消息"

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 5.dp),
    ) {
        val maxBubbleWidth = (maxWidth * 0.78f).coerceAtMost(480.dp)

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (showTimestamp && message.createdTime > 0) {
                Text(
                    text = formatDateTime(message.createdTime),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (incoming) Arrangement.Start else Arrangement.End,
                verticalAlignment = Alignment.Top,
            ) {
                if (incoming) {
                    AsyncImage(
                        model = message.sender?.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Surface(
                    modifier = Modifier.widthIn(max = maxBubbleWidth),
                    shape = RoundedCornerShape(
                        topStart = if (incoming) 4.dp else 16.dp,
                        topEnd = if (incoming) 16.dp else 4.dp,
                        bottomStart = 16.dp,
                        bottomEnd = 16.dp,
                    ),
                    color = if (incoming) {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    },
                ) {
                    Text(
                        text = displayText,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}
