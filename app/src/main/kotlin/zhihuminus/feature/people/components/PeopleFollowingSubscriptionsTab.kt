package com.zhihuminus.feature.people.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.zhihuminus.feature.collection.Collection
import com.zhihuminus.feature.people.FollowedQuestion
import com.zhihuminus.feature.people.FollowedTopic
import com.zhihuminus.feature.people.PaginatedTabState
import com.zhihuminus.feature.people.PeopleColumnItem
import com.zhihuminus.feature.people.PeopleSubscriptionTab
import com.zhihuminus.ui.components.FeedCard
import com.zhihuminus.ui.components.PaginatedList
import com.zhihuminus.ui.components.ProgressIndicatorFooter

@Composable
fun PeopleFollowingSubscriptionsTab(
    selectedIndex: Int,
    onTabSelect: (Int) -> Unit,
    onLoadMore: (Int) -> Unit,
    columnsState: PaginatedTabState<PeopleColumnItem>,
    topicsState: PaginatedTabState<FollowedTopic>,
    questionsState: PaginatedTabState<FollowedQuestion>,
    collectionsState: PaginatedTabState<Collection>,
    onColumnClick: (PeopleColumnItem) -> Unit,
    onTopicClick: (FollowedTopic) -> Unit,
    onQuestionClick: (FollowedQuestion) -> Unit,
    onCollectionClick: (Collection) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PeopleSubscriptionTab.TITLES.forEachIndexed { index, title ->
                val isSelected = selectedIndex == index
                val bg by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent,
                    label = "subTabBg_$index",
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    label = "subTabTextColor_$index",
                )

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(bg)
                        .clickable { onTabSelect(index) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        color = textColor,
                    )
                }
            }
        }

        when (selectedIndex) {
            0 -> {
                if (columnsState.items.isEmpty() && columnsState.hasLoaded && !columnsState.isLoading) {
                    EmptyHint(message = "暂无订阅的专栏")
                } else {
                    PaginatedList(
                        items = columnsState.items,
                        onLoadMore = { onLoadMore(0) },
                        isEnd = { columnsState.isEnd },
                        footer = if (columnsState.isRefreshing) null else ProgressIndicatorFooter,
                        modifier = Modifier.fillMaxSize(),
                        key = { it.id },
                    ) { column ->
                        PeopleColumnListItem(
                            column = column,
                            onClick = { onColumnClick(column) },
                        )
                    }
                }
            }

            1 -> {
                if (topicsState.items.isEmpty() && topicsState.hasLoaded && !topicsState.isLoading) {
                    EmptyHint(message = "暂无关注的话题")
                } else {
                    PaginatedList(
                        items = topicsState.items,
                        onLoadMore = { onLoadMore(1) },
                        isEnd = { topicsState.isEnd },
                        footer = if (topicsState.isRefreshing) null else ProgressIndicatorFooter,
                        modifier = Modifier.fillMaxSize(),
                        key = { it.id },
                    ) { topic ->
                        PeopleFollowedTopicListItem(
                            topic = topic,
                            onClick = { onTopicClick(topic) },
                        )
                    }
                }
            }

            2 -> {
                if (questionsState.items.isEmpty() && questionsState.hasLoaded && !questionsState.isLoading) {
                    EmptyHint(message = "暂无关注的问题")
                } else {
                    PaginatedList(
                        items = questionsState.items,
                        onLoadMore = { onLoadMore(2) },
                        isEnd = { questionsState.isEnd },
                        footer = if (questionsState.isRefreshing) null else ProgressIndicatorFooter,
                        modifier = Modifier.fillMaxSize(),
                        key = { it.id },
                    ) { question ->
                        FeedCard(
                            item = question.toFeedDisplayItem(),
                            modifier = Modifier,
                            onClick = { onQuestionClick(question) },
                        )
                    }
                }
            }

            3 -> {
                if (collectionsState.items.isEmpty() && collectionsState.hasLoaded && !collectionsState.isLoading) {
                    EmptyHint(message = "暂无关注的收藏夹")
                } else {
                    PaginatedList(
                        items = collectionsState.items,
                        onLoadMore = { onLoadMore(3) },
                        isEnd = { collectionsState.isEnd },
                        footer = if (collectionsState.isRefreshing) null else ProgressIndicatorFooter,
                        modifier = Modifier.fillMaxSize(),
                        key = { it.id },
                    ) { collection ->
                        PeopleCollectionListItem(
                            collection = collection,
                            onClick = { onCollectionClick(collection) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyHint(
    message: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
