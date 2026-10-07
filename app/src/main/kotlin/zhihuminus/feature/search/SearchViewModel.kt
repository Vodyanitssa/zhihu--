package com.zhihuminus.feature.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuminus.core.util.Log
import com.zhihuminus.core.util.friendlyErrorMessage
import com.zhihuminus.data.FeedDisplayItem
import com.zhihuminus.data.navDestination
import com.zhihuminus.navigation.Account
import com.zhihuminus.navigation.NavDestination
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val submittedQuery: String = "",
    val searchTab: SearchTab = SearchTab.General,
    val sortOption: SearchSortOption = SearchSortOption.Default,
    val contentType: SearchContentType = SearchContentType.All,
    val timeRange: SearchTimeRange = SearchTimeRange.All,
    val restrictedMemberHashId: String = "",
    val restrictedMemberName: String = "",
    val generalItems: List<FeedDisplayItem> = emptyList(),
    val peopleItems: List<PeopleSearchResult> = emptyList(),
    val topicItems: List<TopicSearchResult> = emptyList(),
    val hotSearchItems: List<HotSearchItem> = emptyList(),
    val searchHistoryItems: List<String> = emptyList(),
    val showHotSearch: Boolean = true,
    val showSearchHistory: Boolean = true,
    val isHotSearchLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isEnd: Boolean = false,
    val errorMessage: String? = null,
    val changingTopicIds: Set<String> = emptySet(),
    val changingPeopleIds: Set<String> = emptySet(),
) {
    val isMemberSearch: Boolean
        get() = restrictedMemberHashId.isNotBlank()
}

class SearchViewModel(
    initialQuery: String,
    restrictedMemberHashId: String = "",
    restrictedMemberName: String = "",
    private val repository: SearchRepository,
) : ViewModel() {
    var uiState by mutableStateOf(
        SearchUiState(
            query = initialQuery,
            submittedQuery = initialQuery,
            restrictedMemberHashId = restrictedMemberHashId,
            restrictedMemberName = restrictedMemberName,
        ),
    )
        private set

    private val _effect = Channel<SearchEffect>(capacity = Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var nextUrl: String? = null
    private var searchJob: Job? = null

    init {
        val showHot = !uiState.isMemberSearch && repository.isHotSearchEnabled()
        val showHistory = !uiState.isMemberSearch && repository.isSearchHistoryEnabled()
        uiState = uiState.copy(
            showHotSearch = showHot,
            showSearchHistory = showHistory,
        )
        loadHistoryAndHot()
        if (initialQuery.isNotBlank()) {
            performSearch(reset = true)
        }
    }

    private fun loadHistoryAndHot() {
        if (!uiState.isMemberSearch) {
            if (uiState.showSearchHistory) {
                val history = repository.getSearchHistory()
                uiState = uiState.copy(searchHistoryItems = history)
            }
            if (uiState.showHotSearch) {
                refreshHotSearch()
            }
        }
    }

    private fun refreshHotSearch() {
        viewModelScope.launch {
            uiState = uiState.copy(isHotSearchLoading = true)
            val hot = repository.fetchHotSearches()
            uiState = uiState.copy(hotSearchItems = hot, isHotSearchLoading = false)
        }
    }

    fun onEvent(event: SearchEvent) {
        when (event) {
            is SearchEvent.QueryChange -> {
                uiState = uiState.copy(
                    query = event.query,
                    submittedQuery = if (event.query.isEmpty()) "" else uiState.submittedQuery,
                )
            }

            is SearchEvent.Submit -> {
                val trimmed = event.query.trim()
                if (trimmed.isEmpty()) return
                uiState = uiState.copy(query = trimmed, submittedQuery = trimmed)
                recordSearchHistory(trimmed)
                viewModelScope.launch {
                    _effect.send(SearchEffect.ClearFocusAndHideKeyboard)
                }
                performSearch(reset = true)
            }

            is SearchEvent.RefreshHotSearch -> {
                refreshHotSearch()
            }

            is SearchEvent.OpenHotSearchSettings -> {
                viewModelScope.launch {
                    _effect.send(SearchEffect.Navigate(Account.AppearanceSettings("showSearchHotSearch")))
                }
            }

            is SearchEvent.OpenSearchHistorySettings -> {
                viewModelScope.launch {
                    _effect.send(SearchEffect.Navigate(Account.AppearanceSettings("showSearchHistory")))
                }
            }

            is SearchEvent.SelectTab -> {
                if (uiState.searchTab == event.tab) return
                uiState = uiState.copy(searchTab = event.tab)
                performSearch(reset = true)
            }

            is SearchEvent.SelectSort -> {
                if (uiState.sortOption == event.sort) return
                uiState = uiState.copy(sortOption = event.sort)
                performSearch(reset = true)
            }

            is SearchEvent.SelectContentType -> {
                if (uiState.contentType == event.type) return
                uiState = uiState.copy(contentType = event.type)
                performSearch(reset = true)
            }

            is SearchEvent.SelectTimeRange -> {
                if (uiState.timeRange == event.range) return
                uiState = uiState.copy(timeRange = event.range)
                performSearch(reset = true)
            }

            is SearchEvent.Refresh -> performSearch(reset = true)
            is SearchEvent.LoadMore -> performSearch(reset = false)

            is SearchEvent.ClearHistory -> {
                repository.clearSearchHistory()
                uiState = uiState.copy(searchHistoryItems = emptyList())
            }

            is SearchEvent.DeleteHistoryItem -> {
                val updated = uiState.searchHistoryItems - event.item
                repository.saveSearchHistory(updated)
                uiState = uiState.copy(searchHistoryItems = updated)
            }

            is SearchEvent.ToggleTopicFollowing -> {
                toggleTopicFollowing(event.topicId, event.following)
            }

            is SearchEvent.TogglePeopleFollowing -> {
                togglePeopleFollowing(event.peopleId, event.urlToken, event.following)
            }

            is SearchEvent.ContentClick -> {
                handleContentClick(event.item, event.destination)
            }

            is SearchEvent.Back -> {
                viewModelScope.launch {
                    _effect.send(SearchEffect.NavigateBack)
                }
            }
        }
    }

    private fun recordSearchHistory(query: String) {
        if (uiState.isMemberSearch || !uiState.showSearchHistory) return
        val updated = (listOf(query) + (uiState.searchHistoryItems - query)).take(20)
        repository.saveSearchHistory(updated)
        uiState = uiState.copy(searchHistoryItems = updated)
    }

    private fun handleContentClick(item: FeedDisplayItem, destination: NavDestination?) {
        val resolvedDestination = destination ?: item.navDestination
        viewModelScope.launch {
            if (resolvedDestination != null) {
                _effect.send(SearchEffect.Navigate(resolvedDestination))
            } else if (item.content?.startsWith("http") == true) {
                _effect.send(SearchEffect.OpenExternalUrl(item.content))
            } else {
                _effect.send(SearchEffect.ShowMessage("暂不支持打开该内容"))
            }
        }
    }

    private fun performSearch(reset: Boolean) {
        val currentQuery = uiState.submittedQuery.ifBlank { uiState.query }
        if (currentQuery.isBlank()) return
        if (reset) {
            nextUrl = null
        } else if (uiState.isLoadingMore || uiState.isRefreshing || uiState.isEnd) {
            return
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (reset) {
                uiState = uiState.copy(isRefreshing = true, errorMessage = null)
            } else {
                uiState = uiState.copy(isLoadingMore = true, errorMessage = null)
            }

            try {
                when (uiState.searchTab) {
                    SearchTab.General -> {
                        val page = repository.searchGeneral(
                            query = currentQuery,
                            tab = uiState.searchTab,
                            sort = uiState.sortOption,
                            contentType = uiState.contentType,
                            timeRange = uiState.timeRange,
                            restrictedMemberHashId = uiState.restrictedMemberHashId,
                            nextUrl = nextUrl,
                        )
                        val current = if (reset) emptyList() else uiState.generalItems
                        val existingKeys = current.map { it.stableKey }.toSet()
                        val combined = current + page.items.filter { it.stableKey !in existingKeys }
                        nextUrl = page.nextUrl
                        uiState = uiState.copy(
                            generalItems = combined,
                            isEnd = page.isEnd,
                            isRefreshing = false,
                            isLoadingMore = false,
                        )
                    }

                    SearchTab.People -> {
                        val page = repository.searchPeople(
                            query = currentQuery,
                            sort = uiState.sortOption,
                            timeRange = uiState.timeRange,
                            restrictedMemberHashId = uiState.restrictedMemberHashId,
                            nextUrl = nextUrl,
                        )
                        val current = if (reset) emptyList() else uiState.peopleItems
                        val existingIds = current.map { it.people.id }.toSet()
                        val combined = current + page.items.filter { it.people.id !in existingIds }
                        nextUrl = page.nextUrl
                        uiState = uiState.copy(
                            peopleItems = combined,
                            isEnd = page.isEnd,
                            isRefreshing = false,
                            isLoadingMore = false,
                        )
                    }

                    SearchTab.Topic -> {
                        val page = repository.searchTopics(
                            query = currentQuery,
                            sort = uiState.sortOption,
                            timeRange = uiState.timeRange,
                            restrictedMemberHashId = uiState.restrictedMemberHashId,
                            nextUrl = nextUrl,
                        )
                        val current = if (reset) emptyList() else uiState.topicItems
                        val existingIds = current.map { it.topic.id }.toSet()
                        val combined = current + page.items.filter { it.topic.id !in existingIds }
                        nextUrl = page.nextUrl
                        uiState = uiState.copy(
                            topicItems = combined,
                            isEnd = page.isEnd,
                            isRefreshing = false,
                            isLoadingMore = false,
                        )
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("SearchViewModel", "Search failed", e)
                val msg = friendlyErrorMessage(e)
                uiState = uiState.copy(
                    isRefreshing = false,
                    isLoadingMore = false,
                    errorMessage = msg,
                )
                _effect.send(SearchEffect.ShowMessage(msg))
            }
        }
    }

    private fun toggleTopicFollowing(topicId: String, following: Boolean) {
        val index = uiState.topicItems.indexOfFirst { it.topic.id == topicId }
        if (index < 0 || topicId in uiState.changingTopicIds) return
        val previous = uiState.topicItems[index]
        uiState = uiState.copy(
            changingTopicIds = uiState.changingTopicIds + topicId,
            topicItems = uiState.topicItems.toMutableList().apply {
                this[index] = previous.copy(isFollowing = following)
            },
        )

        viewModelScope.launch {
            repository.setTopicFollowing(topicId, following).onFailure { error ->
                Log.w("SearchViewModel", "Toggle topic follow failed", error)
                uiState = uiState.copy(
                    topicItems = uiState.topicItems.toMutableList().apply {
                        val curIdx = indexOfFirst { it.topic.id == topicId }
                        if (curIdx >= 0) this[curIdx] = previous
                    },
                )
                _effect.send(SearchEffect.ShowMessage(if (following) "关注话题失败" else "取消关注话题失败"))
            }
            uiState = uiState.copy(changingTopicIds = uiState.changingTopicIds - topicId)
        }
    }

    private fun togglePeopleFollowing(peopleId: String, urlToken: String, following: Boolean) {
        val index = uiState.peopleItems.indexOfFirst { it.people.id == peopleId }
        if (index < 0 || peopleId in uiState.changingPeopleIds) return
        val previous = uiState.peopleItems[index]
        val updatedPeople = previous.people.copy(
            isFollowing = following,
            followerCount = (previous.people.followerCount + if (following) 1 else -1).coerceAtLeast(0),
        )
        uiState = uiState.copy(
            changingPeopleIds = uiState.changingPeopleIds + peopleId,
            peopleItems = uiState.peopleItems.toMutableList().apply {
                this[index] = previous.copy(people = updatedPeople)
            },
        )

        viewModelScope.launch {
            val token = urlToken.ifBlank { peopleId }
            repository.setMemberFollowing(token, following).onFailure { error ->
                Log.w("SearchViewModel", "Toggle people follow failed", error)
                uiState = uiState.copy(
                    peopleItems = uiState.peopleItems.toMutableList().apply {
                        val curIdx = indexOfFirst { it.people.id == peopleId }
                        if (curIdx >= 0) this[curIdx] = previous
                    },
                )
                _effect.send(SearchEffect.ShowMessage(if (following) "关注失败" else "取消关注失败"))
            }
            uiState = uiState.copy(changingPeopleIds = uiState.changingPeopleIds - peopleId)
        }
    }
}
