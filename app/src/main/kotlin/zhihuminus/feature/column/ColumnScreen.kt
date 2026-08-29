package com.zhihuminus.feature.column

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.zhihuminus.ui.components.FeedCard
import com.zhihuminus.ui.components.PaginatedList
import com.zhihuminus.ui.components.ProgressIndicatorFooter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColumnScreen(
    columnId: String,
    state: ColumnUiState,
    onEvent: (ColumnEvent) -> Unit,
    onBack: () -> Unit,
) {
    val listState = rememberLazyListState()
    val pullToRefreshState = rememberPullToRefreshState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("专栏") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { onEvent(ColumnEvent.Refresh) },
            indicator = {
                PullToRefreshDefaults.Indicator(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(innerPadding),
                    isRefreshing = state.isRefreshing,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    state = pullToRefreshState,
                )
            },
            state = pullToRefreshState,
            modifier = Modifier.fillMaxSize(),
        ) {
            PaginatedList(
                items = state.articles,
                onLoadMore = { onEvent(ColumnEvent.LoadMore) },
                isEnd = { state.isEnd },
                key = { it.stableKey },
                listState = listState,
                modifier = Modifier.padding(innerPadding),
                footer = if (state.isRefreshing) null else ProgressIndicatorFooter,
            ) { item ->
                FeedCard(item = item)
            }
        }
    }
}
