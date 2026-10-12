package com.zhihuminus.feature.watchlater

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zhihuminus.feature.watchlater.components.WatchLaterCard
import com.zhihuminus.feature.watchlater.components.WatchLaterClearDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchLaterScreen(
    state: WatchLaterUiState,
    onEvent: (WatchLaterEvent) -> Unit,
    modifier: Modifier = Modifier,
    innerPadding: PaddingValues = PaddingValues(0.dp),
    listState: LazyListState = rememberLazyListState(),
    onNavigateBack: (() -> Unit)? = null,
) {
    var showClearDialog by remember { mutableStateOf(false) }
    var showActionsMenu by remember { mutableStateOf(false) }

    BackHandler(enabled = showClearDialog) {
        showClearDialog = false
    }

    BackHandler(enabled = showActionsMenu) {
        showActionsMenu = false
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("稍后再看") },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回",
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showActionsMenu = true }) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = "更多选项",
                        )

                        DropdownMenu(
                            expanded = showActionsMenu,
                            onDismissRequest = { showActionsMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("清除稍后再看列表") },
                                onClick = {
                                    showActionsMenu = false
                                    showClearDialog = true
                                },
                            )
                        }
                    }
                },
            )
        },
    ) { scaffoldPadding ->
        if (showClearDialog) {
            WatchLaterClearDialog(
                onConfirm = {
                    showClearDialog = false
                    onEvent(WatchLaterEvent.ClearAll)
                },
                onDismiss = { showClearDialog = false },
            )
        }

        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { onEvent(WatchLaterEvent.Refresh) },
            modifier = Modifier
                .fillMaxSize()
                .padding(top = scaffoldPadding.calculateTopPadding()),
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

                state.items.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = innerPadding.calculateBottomPadding()),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "暂无稍后再看内容",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(
                            bottom = innerPadding.calculateBottomPadding() + 16.dp,
                        ),
                    ) {
                        items(
                            items = state.items,
                            key = { "${it.type}:${it.id}" },
                        ) { item ->
                            WatchLaterCard(
                                item = item,
                                onClick = { onEvent(WatchLaterEvent.ContentClick(item)) },
                                onDelete = { onEvent(WatchLaterEvent.DeleteItem(it)) },
                            )
                        }
                    }
                }
            }
        }
    }
}
