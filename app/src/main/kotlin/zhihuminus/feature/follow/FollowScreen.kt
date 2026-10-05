package com.zhihuminus.feature.follow

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.zhihuminus.feature.follow.components.FollowingUsersRow
import com.zhihuminus.ui.components.FeedCard
import com.zhihuminus.ui.components.PaginatedList
import com.zhihuminus.ui.components.ProgressIndicatorFooter

@Composable
fun FollowScreen(
    state: FollowUiState,
    innerPadding: PaddingValues,
    listState: LazyListState,
    onEvent: (FollowEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val readingQueueSourceId = "follow:dynamic"

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding(),
            ),
    ) {
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { onEvent(FollowEvent.Refresh) },
            modifier = Modifier.fillMaxSize(),
        ) {
            PaginatedList(
                items = state.items,
                listState = listState,
                modifier = Modifier,
                onLoadMore = { onEvent(FollowEvent.LoadMore) },
                isEnd = { state.isEnd },
                topContent = {
                    item(key = "following_users_row") {
                        FollowingUsersRow(
                            users = state.users,
                            errorMessage = state.usersErrorMessage,
                            onUserClick = { user -> onEvent(FollowEvent.UserClick(user)) },
                        )
                    }
                },
                footer = if (!state.isRefreshing && state.isLoadingMore) ProgressIndicatorFooter else null,
                key = { item -> item.stableKey },
            ) { item ->
                FeedCard(
                    item = item,
                    readingQueueSourceId = readingQueueSourceId,
                    modifier = Modifier,
                    showSourceLabel = true,
                    onClick = { clickedItem, destination ->
                        onEvent(FollowEvent.ContentClick(clickedItem, destination))
                    },
                )
            }
        }
    }
}
