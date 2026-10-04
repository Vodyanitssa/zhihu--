package com.zhihuminus.feature.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import com.zhihuminus.feature.search.components.SearchResultsContent
import com.zhihuminus.feature.search.components.SearchSuggestionsContent
import com.zhihuminus.feature.search.components.SearchTopBar

@Composable
fun SearchScreen(
    state: SearchUiState,
    generalListState: LazyListState,
    peopleListState: LazyListState,
    topicListState: LazyListState,
    focusRequester: FocusRequester,
    onEvent: (SearchEvent) -> Unit,
) {
    Scaffold(
        topBar = {
            SearchTopBar(
                query = state.query,
                placeholder = if (state.isMemberSearch) {
                    "搜索 ${state.restrictedMemberName.ifBlank { "TA" }} 的创作"
                } else {
                    "搜索内容"
                },
                focusRequester = focusRequester,
                onQueryChange = { onEvent(SearchEvent.QueryChange(it)) },
                onSearch = { onEvent(SearchEvent.Submit(it)) },
                onBack = { onEvent(SearchEvent.Back) },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (state.submittedQuery.isBlank()) {
                SearchSuggestionsContent(
                    historyItems = state.searchHistoryItems,
                    hotItems = state.hotSearchItems,
                    showSearchHistory = state.showSearchHistory,
                    showHotSearch = state.showHotSearch,
                    isMemberSearch = state.isMemberSearch,
                    memberSearchName = state.restrictedMemberName,
                    onItemClick = { onEvent(SearchEvent.Submit(it)) },
                    onRefreshHotSearch = { onEvent(SearchEvent.RefreshHotSearch) },
                    onClearHistory = { onEvent(SearchEvent.ClearHistory) },
                    onOpenHotSearchSettings = { onEvent(SearchEvent.OpenHotSearchSettings) },
                    onOpenSearchHistorySettings = { onEvent(SearchEvent.OpenSearchHistorySettings) },
                )
            } else {
                SearchResultsContent(
                    state = state,
                    generalListState = generalListState,
                    peopleListState = peopleListState,
                    topicListState = topicListState,
                    onEvent = onEvent,
                )
            }
        }
    }
}
