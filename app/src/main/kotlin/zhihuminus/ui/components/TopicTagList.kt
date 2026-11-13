package com.zhihuminus.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class TopicTag(
    val id: String,
    val name: String,
)

@Composable
fun TopicTagList(
    topics: List<TopicTag>,
    onTopicClick: (TopicTag) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        topics.forEach { topic ->
            SuggestionChip(
                onClick = { onTopicClick(topic) },
                label = { Text("# ${topic.name}") },
            )
        }
    }
}
