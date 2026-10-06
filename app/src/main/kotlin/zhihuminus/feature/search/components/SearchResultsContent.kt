package com.zhihuminus.feature.search.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zhihuminus.data.FeedDisplayItem
import com.zhihuminus.feature.search.SearchEvent
import com.zhihuminus.feature.search.SearchTab
import com.zhihuminus.feature.search.SearchUiState
import com.zhihuminus.navigation.Person
import com.zhihuminus.navigation.Topic
import com.zhihuminus.ui.components.FeedCard
import com.zhihuminus.ui.components.PaginatedList
import com.zhihuminus.ui.components.PeopleListItem
import com.zhihuminus.ui.components.ProgressIndicatorFooter

@Composable
fun SearchResultsContent(
    state: SearchUiState,
    generalListState: LazyListState,
    peopleListState: LazyListState,
    topicListState: LazyListState,
    onEvent: (SearchEvent) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (!state.isMemberSearch) {
            PrimaryTabRow(
                selectedTabIndex = state.searchTab.ordinal,
                modifier = Modifier.fillMaxWidth(),
            ) {
                SearchTab.entries.forEach { tab ->
                    Tab(
                        selected = state.searchTab == tab,
                        onClick = { onEvent(SearchEvent.SelectTab(tab)) },
                        text = { Text(tab.label) },
                    )
                }
            }
        }

        if (state.searchTab == SearchTab.General) {
            SearchFilterBar(
                sortOption = state.sortOption,
                contentType = state.contentType,
                timeRange = state.timeRange,
                onSortChange = { onEvent(SearchEvent.SelectSort(it)) },
                onContentTypeChange = { onEvent(SearchEvent.SelectContentType(it)) },
                onTimeRangeChange = { onEvent(SearchEvent.SelectTimeRange(it)) },
            )
        }

        when (state.searchTab) {
            SearchTab.General -> {
                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = { onEvent(SearchEvent.Refresh) },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    PaginatedList(
                        items = state.generalItems,
                        listState = generalListState,
                        onLoadMore = { onEvent(SearchEvent.LoadMore) },
                        isEnd = { state.isEnd },
                        footer = if (!state.isRefreshing && state.isLoadingMore) ProgressIndicatorFooter else null,
                        key = { it.stableKey },
                    ) { item ->
                        FeedCard(
                            item = item,
                            onClick = {
                                onEvent(SearchEvent.ContentClick(item))
                            },
                        )
                    }
                }
            }

            SearchTab.People -> {
                if (state.isRefreshing && state.peopleItems.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        state = peopleListState,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(state.peopleItems, key = { it.people.id }) { result ->
                            val people = result.people
                            PeopleListItem(
                                people = people,
                                highlightedName = result.highlightedName,
                                isFollowing = people.isFollowing,
                                isChangingFollowing = people.id in state.changingPeopleIds,
                                onClick = {
                                    onEvent(
                                        SearchEvent.ContentClick(
                                            item = FeedDisplayItem(
                                                title = people.name,
                                                summary = people.headline,
                                                details = "",
                                            ),
                                            destination = Person(
                                                id = people.id,
                                                urlToken = people.urlToken.orEmpty(),
                                                name = people.name,
                                            ),
                                        ),
                                    )
                                },
                                onToggleFollow = {
                                    onEvent(
                                        SearchEvent.TogglePeopleFollowing(
                                            peopleId = people.id,
                                            urlToken = people.urlToken.orEmpty(),
                                            following = !people.isFollowing,
                                        ),
                                    )
                                },
                            )
                            HorizontalDivider(thickness = 0.5.dp)
                        }
                        if (state.isLoadingMore) {
                            item { ProgressIndicatorFooter(peopleListState) }
                        } else if (state.isEnd && state.peopleItems.isNotEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "已经到底啦",
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            SearchTab.Topic -> {
                if (state.isRefreshing && state.topicItems.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        state = topicListState,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(state.topicItems, key = { it.topic.id }) { result ->
                            val topic = result.topic
                            SearchTopicItem(
                                result = result,
                                isChangingFollowing = topic.id in state.changingTopicIds,
                                onClick = {
                                    onEvent(
                                        SearchEvent.ContentClick(
                                            item = FeedDisplayItem(
                                                title = topic.name,
                                                summary = result.excerpt,
                                                details = "",
                                            ),
                                            destination = Topic(
                                                id = topic.id,
                                                name = topic.name,
                                            ),
                                        ),
                                    )
                                },
                                onToggleFollowing = { following ->
                                    onEvent(SearchEvent.ToggleTopicFollowing(topic.id, following))
                                },
                            )
                            HorizontalDivider(thickness = 0.5.dp)
                        }
                        if (state.isLoadingMore) {
                            item { ProgressIndicatorFooter(topicListState) }
                        } else if (state.isEnd && state.topicItems.isNotEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "已经到底啦",
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
