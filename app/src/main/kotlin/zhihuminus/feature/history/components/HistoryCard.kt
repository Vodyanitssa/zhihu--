package com.zhihuminus.feature.history.components

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.zhihuminus.data.FeedDisplayItem
import com.zhihuminus.data.toFeedDisplayItemNavDestinationJson
import com.zhihuminus.feature.history.HistoryItem
import com.zhihuminus.navigation.resolveContent
import com.zhihuminus.ui.components.FeedCard

@Composable
fun HistoryCard(
    item: HistoryItem,
    onDelete: (HistoryItem) -> Unit,
    isActive: Boolean = true,
) {
    val displayItem = remember(item) {
        val navDest = resolveContent(item.actionUrl)
        FeedDisplayItem(
            title = item.title,
            summary = item.summary,
            details = item.details,
            feed = null,
            navDestinationJson = navDest?.toFeedDisplayItemNavDestinationJson(),
            avatarSrc = null,
            authorName = item.authorName,
            contentTypeLabel = item.contentTypeLabel,
            content = item.actionUrl,
        )
    }

    FeedCard(
        item = displayItem,
        readingQueueSourceId = "history:online".takeIf { isActive },
        menuItems = { dismissMenu ->
            DropdownMenuItem(
                text = { Text("删除该条历史记录") },
                onClick = {
                    dismissMenu()
                    onDelete(item)
                },
            )
        },
    )
}
