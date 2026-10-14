package com.zhihuminus.feature.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zhihuminus.feature.home.components.HomeLoginPrompt
import com.zhihuminus.feature.home.components.HomeTopBar
import com.zhihuminus.ui.AccountSettingsAccountState
import com.zhihuminus.ui.components.FeedCard
import com.zhihuminus.ui.components.PaginatedList
import com.zhihuminus.ui.components.ProgressIndicatorFooter
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    account: AccountSettingsAccountState,
    showUnreadBadge: Boolean,
    innerPadding: PaddingValues,
    listState: LazyListState,
    onEvent: (HomeEvent) -> Unit,
    onRequestLogin: () -> Unit,
) {
    val pullToRefreshState = rememberPullToRefreshState()

    val visibleItems by remember(state.items, listState) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleInfo = layoutInfo.visibleItemsInfo
            if (visibleInfo.isEmpty()) {
                emptyList()
            } else {
                val totalItems = state.items
                visibleInfo.mapNotNull { info ->
                    totalItems.getOrNull(info.index)
                }
            }
        }
    }

    LaunchedEffect(visibleItems) {
        if (visibleItems.isNotEmpty()) {
            delay(400.milliseconds)
            onEvent(HomeEvent.ItemsExposed(visibleItems))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                HomeTopBar(
                    avatarUrl = account.avatarUrl,
                    unreadCount = state.unreadCount,
                    showUnreadBadge = showUnreadBadge,
                    onSearchClick = { onEvent(HomeEvent.SearchClick) },
                    onAvatarClick = { onEvent(HomeEvent.AvatarClick) },
                )
            },
        ) { scaffoldPadding ->
            if (!account.login) {
                HomeLoginPrompt(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(scaffoldPadding)
                        .padding(horizontal = 32.dp),
                    onRequestLogin = onRequestLogin,
                )
            } else {
                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = { onEvent(HomeEvent.Refresh) },
                    state = pullToRefreshState,
                    indicator = {
                        PullToRefreshDefaults.Indicator(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = scaffoldPadding.calculateTopPadding()),
                            isRefreshing = state.isRefreshing,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            state = pullToRefreshState,
                        )
                    },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    PaginatedList(
                        items = state.items,
                        listState = listState,
                        modifier = Modifier,
                        contentPadding = PaddingValues(
                            top = scaffoldPadding.calculateTopPadding() + 8.dp,
                            bottom = innerPadding.calculateBottomPadding(),
                        ),
                        onLoadMore = { onEvent(HomeEvent.LoadMore) },
                        isEnd = { state.isEnd },
                        footer = if (!state.isRefreshing && state.isLoadingMore) ProgressIndicatorFooter else null,
                        key = { item -> item.stableKey },
                    ) { item ->
                        FeedCard(
                            item = item,
                            onClick = {
                                onEvent(HomeEvent.ContentClick(item))
                            },
                            menuItems = { dismissMenu ->
                                DropdownMenuItem(
                                    text = { Text("添加到稍后再看") },
                                    onClick = {
                                        dismissMenu()
                                        onEvent(HomeEvent.AddToWatchLater(item))
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("不感兴趣") },
                                    onClick = {
                                        dismissMenu()
                                        onEvent(HomeEvent.MarkUninterested(item))
                                    },
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}
