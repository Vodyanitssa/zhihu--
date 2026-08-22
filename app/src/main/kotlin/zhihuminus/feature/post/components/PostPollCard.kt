package com.zhihuminus.feature.post.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.zhihuminus.feature.post.PostPoll
import com.zhihuminus.feature.post.PostPollOption

@Composable
fun PostPollCard(
    poll: PostPoll,
    onPollVote: (pollId: String, optionId: String) -> Unit,
) {
    val acceptsVote = poll.acceptsVote()
    val showsResult = poll.isVoted || !acceptsVote
    val pollVoterCount = poll.memberCount.takeIf { it > 0 } ?: poll.votingCount

    Spacer(modifier = Modifier.height(16.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = poll.title.ifBlank { "想法投票" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = poll.statusText(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            poll.options.forEach { option ->
                if (showsResult) {
                    PollResultRow(
                        option = option,
                        totalVoterCount = pollVoterCount,
                    )
                } else {
                    FilledTonalButton(
                        onClick = { onPollVote(poll.id, option.id) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = option.title,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun PollResultRow(
    option: PostPollOption,
    totalVoterCount: Int,
) {
    val voteFraction = if (totalVoterCount > 0) {
        (option.votingCount.toFloat() / totalVoterCount).coerceIn(0f, 1f)
    } else {
        0f
    }
    val rowShape = RoundedCornerShape(12.dp)
    val indicatorColor = if (option.isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = rowShape,
        color = if (option.isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    if (voteFraction > 0f) {
                        drawRoundRect(
                            color = indicatorColor,
                            size = Size(
                                width = size.width * voteFraction,
                                height = size.height,
                            ),
                            cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
                        )
                    }
                }.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = option.title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (option.isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.weight(1f),
            )
            if (option.isSelected) {
                Text(
                    text = "已选",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = "${option.votingCount} 票",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
