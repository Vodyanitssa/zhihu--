package com.zhihuminus.feature.collection.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zhihuminus.core.util.formatCount
import com.zhihuminus.data.FeedDisplayItem
import com.zhihuminus.data.navDestination
import com.zhihuminus.feature.collection.CollectionContentUiState
import com.zhihuminus.feature.collection.PendingCollectionItemRemoval
import com.zhihuminus.feature.post.PostType
import com.zhihuminus.navigation.NavDestination
import com.zhihuminus.navigation.Person
import com.zhihuminus.navigation.PostDestination
import com.zhihuminus.navigation.withReadingQueueSource
import com.zhihuminus.ui.components.ErrorInline
import com.zhihuminus.ui.components.FeedCard
import com.zhihuminus.ui.components.PaginatedList
import com.zhihuminus.ui.components.ProgressIndicatorFooter

@Composable
fun CollectionContentBody(
    state: CollectionContentUiState,
    isOwner: Boolean,
    listState: LazyListState,
    isDescriptionExpanded: Boolean,
    onDescriptionExpandedChange: (Boolean) -> Unit,
    onRetryDetail: () -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onDestinationClick: ((NavDestination?) -> Unit)?,
    onRequestRemoveItem: (PendingCollectionItemRemoval) -> Unit,
    modifier: Modifier = Modifier,
) {
    val readingQueueSourceId = "collection:${state.collection?.id.orEmpty()}:contents"
    val itemsError = state.errorMessage
    val contentCount = state.totalItems?.takeIf { it > 0 }
        ?: state.collection?.itemCount?.takeIf { it > 0 }
        ?: state.items.size
    val creator = state.collection?.creator

    PaginatedList(
        items = state.items,
        onLoadMore = onLoadMore,
        isEnd = { state.isEnd },
        listState = listState,
        modifier = modifier,
        key = { it.stableKey },
        showEndNotice = state.items.isNotEmpty(),
        footer = if (state.isRefreshing || itemsError != null) null else ProgressIndicatorFooter,
        topContent = {
            item(key = "collection_header") {
                CollectionHeader(
                    collection = state.collection,
                    detailErrorMessage = state.detailErrorMessage,
                    isDescriptionExpanded = isDescriptionExpanded,
                    onDescriptionExpandedChange = onDescriptionExpandedChange,
                    onRetryDetail = onRetryDetail,
                )
            }
            item(key = "collection_header_divider") {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
            }
            if (creator != null && creator.name.isNotBlank()) {
                item(key = "collection_creator_header") {
                    CollectionSectionHeader("创建者")
                }
                item(key = "collection_creator_card") {
                    CollectionCreatorCard(
                        creator = creator,
                        onClick = {
                            onDestinationClick?.invoke(
                                Person(
                                    id = creator.id,
                                    urlToken = "",
                                    name = creator.name,
                                ),
                            )
                        },
                    )
                }
            }
            item(key = "collection_content_header") {
                CollectionSectionHeader("共 ${formatCount(contentCount)} 个内容")
            }
        },
        bottomContent = {
            if (state.items.isEmpty()) {
                item(key = "collection_empty_state") {
                    if (itemsError != null) {
                        ErrorInline(
                            error = itemsError,
                            onRetry = onRefresh,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                        )
                    } else if (state.isEnd && !state.isRefreshing && !state.isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("这个收藏夹是空的")
                        }
                    }
                }
            }
        },
    ) { item ->
        FeedCard(
            item = item,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            onClick = {
                val destination = item.navDestination?.withReadingQueueSource(readingQueueSourceId)
                onDestinationClick?.invoke(destination)
            },
            menuItems = { dismissMenu ->
                if (isOwner) {
                    val target = item.toRemovalTarget()
                    if (target != null) {
                        DropdownMenuItem(
                            text = { Text("移出收藏夹") },
                            onClick = {
                                dismissMenu()
                                onRequestRemoveItem(target)
                            },
                        )
                    }
                }
            },
        )
    }
}

@Composable
private fun CollectionSectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp),
    )
}

private fun FeedDisplayItem.toRemovalTarget(): PendingCollectionItemRemoval? {
    val destination = navDestination as? PostDestination ?: return null
    val type = when (destination.type) {
        PostType.Answer -> "answer"
        PostType.Article -> "article"
        PostType.Pin -> "pin"
    }
    return PendingCollectionItemRemoval(
        type = type,
        id = destination.id,
        stableKey = stableKey,
        title = title,
    )
}
