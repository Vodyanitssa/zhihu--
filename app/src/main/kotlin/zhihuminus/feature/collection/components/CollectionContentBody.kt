package com.zhihuminus.feature.collection.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastJoinToString
import com.zhihuminus.core.util.formatDateTime
import com.zhihuminus.data.FeedDisplayItem
import com.zhihuminus.feature.collection.Collection
import com.zhihuminus.navigation.NavDestination
import com.zhihuminus.ui.components.FeedCard
import com.zhihuminus.ui.components.PaginatedList
import com.zhihuminus.ui.components.ProgressIndicatorFooter

@Composable
fun CollectionContentBody(
    items: List<FeedDisplayItem>,
    collection: Collection?,
    onLoadMore: () -> Unit,
    isEnd: Boolean,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    showStatsHeader: Boolean = true,
    onDestinationClick: ((NavDestination?) -> Unit)? = null,
) {
    val readingQueueSourceId = "collection:${collection?.id.orEmpty()}:contents"

    PaginatedList(
        items = items,
        onLoadMore = onLoadMore,
        isEnd = { isEnd },
        listState = listState,
        modifier = modifier,
        footer = ProgressIndicatorFooter,
        topContent = {
            if (showStatsHeader && collection != null) {
                item {
                    val stats = listOfNotNull(
                        collection.itemCount.let { "$it 条收藏" },
                        collection.likeCount.let { "$it 个赞同" },
                        collection.commentCount.let { "$it 条评论" },
                        collection.updatedTime.takeIf { it > 0 }?.let { "${formatDateTime(it)} 更新" },
                    )
                    Text(
                        text = stats.fastJoinToString(" · "),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        },
    ) { item ->
        FeedCard(
            item = item,
            readingQueueSourceId = readingQueueSourceId,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            onClick = onDestinationClick?.let { callback ->
                { _, destination -> callback(destination) }
            },
        )
    }
}
