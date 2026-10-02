package com.zhihuminus.feature.history

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zhihuminus.feature.history.components.HistoryCard
import com.zhihuminus.feature.history.components.HistoryClearDialog
import com.zhihuminus.platform.PlatformBackHandler
import com.zhihuminus.ui.TopLevelReselectAction
import com.zhihuminus.ui.components.PaginatedList
import com.zhihuminus.ui.topLevelReselectAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onEvent: (HistoryEvent) -> Unit,
    scrollToTopTrigger: Int = 0,
    isActive: Boolean = true,
) {
    val listState = rememberLazyListState()
    var cachedScrollToTopTrigger by remember { mutableIntStateOf(scrollToTopTrigger) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    LaunchedEffect(scrollToTopTrigger, isActive) {
        val action = topLevelReselectAction(
            triggerDelta = scrollToTopTrigger - cachedScrollToTopTrigger,
            isAtTop = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0,
        )
        if (isActive) {
            when (action) {
                TopLevelReselectAction.Refresh -> onEvent(HistoryEvent.Refresh)
                TopLevelReselectAction.ScrollToTop -> listState.animateScrollToItem(0)
                null -> {}
            }
        }
        cachedScrollToTopTrigger = scrollToTopTrigger
    }

    PlatformBackHandler(enabled = showClearHistoryDialog) {
        showClearHistoryDialog = false
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("历史记录") },
                actions = {
                    var showActionsMenu by remember { mutableStateOf(false) }
                    PlatformBackHandler(enabled = showActionsMenu) {
                        showActionsMenu = false
                    }
                    IconButton(
                        modifier = Modifier,
                        onClick = { showActionsMenu = true },
                    ) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = "更多选项",
                        )

                        DropdownMenu(
                            expanded = showActionsMenu,
                            onDismissRequest = { showActionsMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("清除历史记录") },
                                onClick = {
                                    showActionsMenu = false
                                    showClearHistoryDialog = true
                                },
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        if (showClearHistoryDialog) {
            HistoryClearDialog(
                onConfirm = {
                    showClearHistoryDialog = false
                    onEvent(HistoryEvent.ClearAll)
                },
                onDismiss = { showClearHistoryDialog = false },
            )
        }

        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { onEvent(HistoryEvent.Refresh) },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                state.isLoading && state.items.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                state.errorMessage != null && state.items.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = state.errorMessage,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                            TextButton(onClick = { onEvent(HistoryEvent.Refresh) }) {
                                Text("重试")
                            }
                        }
                    }
                }

                state.items.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "暂无历史记录",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                }

                else -> {
                    PaginatedList(
                        modifier = Modifier.fillMaxSize(),
                        items = state.items,
                        listState = listState,
                        onLoadMore = { onEvent(HistoryEvent.LoadMore) },
                        isEnd = { state.isEnd },
                        key = { "${it.contentType}:${it.contentToken}" },
                        footer = {
                            if (state.isLoadingMore) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        },
                    ) { item ->
                        HistoryCard(
                            item = item,
                            onDelete = { onEvent(HistoryEvent.DeleteItem(it)) },
                            isActive = isActive,
                        )
                    }
                }
            }
        }
    }
}
