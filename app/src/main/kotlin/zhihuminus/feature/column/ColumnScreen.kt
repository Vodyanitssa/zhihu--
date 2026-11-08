package com.zhihuminus.feature.column

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.zhihuminus.feature.column.components.ColumnAuthorCard
import com.zhihuminus.feature.column.components.ColumnHeader
import com.zhihuminus.feature.column.components.ColumnSectionHeader
import com.zhihuminus.ui.components.ErrorFullPage
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
    var isIntroductionExpanded by rememberSaveable(columnId) { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(state.detail?.title?.takeIf { it.isNotBlank() } ?: "专栏") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { innerPadding ->
        val error = state.errorMessage
        if (error != null && state.articles.isEmpty()) {
            ErrorFullPage(
                error = error,
                onRetry = { onEvent(ColumnEvent.Refresh) },
                modifier = Modifier.padding(innerPadding),
            )
        } else {
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
                    topContent = {
                        item(key = "column_header") {
                            ColumnHeader(
                                detail = state.detail,
                                detailErrorMessage = state.detailErrorMessage,
                                isFollowingChanging = state.isFollowingChanging,
                                isIntroductionExpanded = isIntroductionExpanded,
                                onExpandedChange = { isIntroductionExpanded = it },
                                onRetryDetail = { onEvent(ColumnEvent.RetryDetail) },
                                onFollowingChange = { onEvent(ColumnEvent.Follow(it)) },
                            )
                        }
                        item(key = "column_header_divider") {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                        }
                        val author = state.detail?.author
                        if (author != null && (author.name.isNotBlank() || author.urlToken.isNotBlank())) {
                            item(key = "column_author_header") {
                                ColumnSectionHeader("专栏作者")
                            }
                            item(key = "column_author_card") {
                                ColumnAuthorCard(
                                    author = author,
                                    isFollowingChanging = state.isAuthorFollowingChanging,
                                    onClick = {
                                        onEvent(ColumnEvent.AuthorClick(author.id, author.urlToken, author.name))
                                    },
                                    onFollowClick = {
                                        onEvent(ColumnEvent.FollowAuthor(author.isFollowing != true))
                                    },
                                )
                            }
                        }
                        item(key = "column_articles_header") {
                            ColumnSectionHeader("已更内容")
                        }
                    },
                ) { item ->
                    FeedCard(
                        item = item,
                        showAuthor = false,
                        onClick = {
                            onEvent(ColumnEvent.ContentClick(item))
                        },
                    )
                }
            }
        }
    }
}
