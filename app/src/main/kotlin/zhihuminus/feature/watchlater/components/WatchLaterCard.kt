package com.zhihuminus.feature.watchlater.components

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.zhihuminus.core.util.formatRelativeTime
import com.zhihuminus.data.FeedDisplayItem
import com.zhihuminus.data.FeedImageLayout
import com.zhihuminus.data.toFeedDisplayItemNavDestinationJson
import com.zhihuminus.feature.watchlater.WatchLaterItem
import com.zhihuminus.ui.components.FeedCard

@Composable
fun WatchLaterCard(
    item: WatchLaterItem,
    onClick: () -> Unit,
    onDelete: (WatchLaterItem) -> Unit,
) {
    val displayItem = remember(item) {
        val images = listOfNotNull(item.coverImage)
        val layout = if (images.isNotEmpty()) {
            if (item.type.lowercase() == "pin") FeedImageLayout.BOTTOM_GALLERY else FeedImageLayout.RIGHT_THUMBNAIL
        } else {
            FeedImageLayout.NONE
        }
        val details = if (item.addedAt > 0) {
            "添加于 ${formatRelativeTime(item.addedAt / 1000)}"
        } else {
            ""
        }
        FeedDisplayItem(
            title = item.title,
            summary = item.excerpt,
            details = details,
            feed = null,
            navDestinationJson = item.toDestination().toFeedDisplayItemNavDestinationJson(),
            avatarSrc = item.authorAvatar,
            authorName = item.authorName,
            contentTypeLabel = item.contentTypeLabel,
            content = item.actionUrl,
            images = images,
            imageLayout = layout,
        )
    }

    FeedCard(
        item = displayItem,
        onClick = onClick,
        menuItems = { dismissMenu ->
            DropdownMenuItem(
                text = { Text("从稍后再看中删除") },
                onClick = {
                    dismissMenu()
                    onDelete(item)
                },
            )
        },
    )
}
