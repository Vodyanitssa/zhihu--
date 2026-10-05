package com.zhihuminus.feature.search

import com.zhihuminus.data.FeedDisplayItem
import com.zhihuminus.navigation.NavDestination

sealed interface SearchEvent {
    data class QueryChange(
        val query: String,
    ) : SearchEvent

    data class Submit(
        val query: String,
    ) : SearchEvent

    data class SelectTab(
        val tab: SearchTab,
    ) : SearchEvent

    data class SelectSort(
        val sort: SearchSortOption,
    ) : SearchEvent

    data class SelectContentType(
        val type: SearchContentType,
    ) : SearchEvent

    data class SelectTimeRange(
        val range: SearchTimeRange,
    ) : SearchEvent

    data object Refresh : SearchEvent

    data object LoadMore : SearchEvent

    data object ClearHistory : SearchEvent

    data object RefreshHotSearch : SearchEvent

    data object OpenHotSearchSettings : SearchEvent

    data object OpenSearchHistorySettings : SearchEvent

    data class DeleteHistoryItem(
        val item: String,
    ) : SearchEvent

    data class ToggleTopicFollowing(
        val topicId: String,
        val following: Boolean,
    ) : SearchEvent

    data class TogglePeopleFollowing(
        val peopleId: String,
        val urlToken: String,
        val following: Boolean,
    ) : SearchEvent

    data class ContentClick(
        val item: FeedDisplayItem,
        val destination: NavDestination?,
    ) : SearchEvent

    data object Back : SearchEvent
}

sealed interface SearchEffect {
    data class Navigate(
        val destination: NavDestination,
    ) : SearchEffect

    data class ShowMessage(
        val message: String,
    ) : SearchEffect

    data class OpenExternalUrl(
        val url: String,
    ) : SearchEffect

    data object ClearFocusAndHideKeyboard : SearchEffect

    data object NavigateBack : SearchEffect
}
