package com.zhihuminus.feature.topic

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuminus.data.FeedDisplayItem
import com.zhihuminus.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class TopicUiState(
    val detail: TopicDetail? = null,
    val items: List<FeedDisplayItem> = emptyList(),
    val selectedTab: TopicFeedTab = TopicFeedTab.Discussion,
    val discussionSort: TopicDiscussionSort = TopicDiscussionSort.Hot,
    val ideasSort: TopicIdeasSort = TopicIdeasSort.Hot,
    val isLoading: Boolean = false,
    val isLoadingDetail: Boolean = false,
    val errorMessage: String? = null,
    val detailErrorMessage: String? = null,
    val isEnd: Boolean = false,
    val isFollowingChanging: Boolean = false,
)

class TopicViewModel(
    private val topicId: String,
    private val initialName: String,
    private val repository: TopicRepository,
) : ViewModel() {
    var uiState by mutableStateOf(TopicUiState())
        private set

    private val _effect = Channel<TopicEffect>(capacity = Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var nextUrl: String? = null
    private var requestGeneration = 0L
    private var loadJob: Job? = null

    init {
        loadDetail()
        loadMore()
    }

    fun onEvent(event: TopicEvent) {
        when (event) {
            is TopicEvent.InitializeSection -> initializeSection(event.section)
            is TopicEvent.SelectTab -> selectTab(event.tab)
            is TopicEvent.SelectDiscussionSort -> selectDiscussionSort(event.sort)
            is TopicEvent.SelectIdeasSort -> selectIdeasSort(event.sort)
            is TopicEvent.LoadMore -> loadMore()
            is TopicEvent.Retry -> retry()
            is TopicEvent.Follow -> follow(event.following)
        }
    }

    private fun initializeSection(section: String) {
        val tab = when (section) {
            "unanswered" -> TopicFeedTab.Unanswered
            else -> TopicFeedTab.Discussion
        }
        val sort = when (section) {
            "top-answers" -> TopicDiscussionSort.Essence
            "newest" -> TopicDiscussionSort.Timeline
            else -> TopicDiscussionSort.Hot
        }
        if (uiState.selectedTab == tab && uiState.discussionSort == sort) return
        loadJob?.cancel()
        loadJob = null
        uiState = uiState.copy(
            selectedTab = tab,
            discussionSort = sort,
            items = emptyList(),
            errorMessage = null,
            isEnd = false,
            isLoading = false,
        )
        nextUrl = null
        requestGeneration++
        loadMore()
    }

    private fun loadDetail() {
        uiState = uiState.copy(isLoadingDetail = true, detailErrorMessage = null)
        viewModelScope.launch {
            try {
                val detail = repository.getTopicDetail(topicId)
                uiState = uiState.copy(detail = detail, isLoadingDetail = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("TopicViewModel", "Failed to load topic detail", e)
                uiState = uiState.copy(
                    isLoadingDetail = false,
                    detailErrorMessage = e.message,
                )
            }
        }
    }

    private fun selectTab(tab: TopicFeedTab) {
        if (uiState.selectedTab == tab && uiState.items.isNotEmpty()) return
        loadJob?.cancel()
        loadJob = null
        uiState = uiState.copy(
            selectedTab = tab,
            items = emptyList(),
            errorMessage = null,
            isEnd = false,
            isLoading = false,
        )
        nextUrl = null
        requestGeneration++
        loadMore()
    }

    private fun selectDiscussionSort(sort: TopicDiscussionSort) {
        if (uiState.discussionSort == sort) return
        loadJob?.cancel()
        loadJob = null
        uiState = uiState.copy(
            discussionSort = sort,
            items = emptyList(),
            errorMessage = null,
            isEnd = false,
            isLoading = false,
        )
        nextUrl = null
        requestGeneration++
        loadMore()
    }

    private fun selectIdeasSort(sort: TopicIdeasSort) {
        if (uiState.ideasSort == sort) return
        loadJob?.cancel()
        loadJob = null
        uiState = uiState.copy(
            ideasSort = sort,
            items = emptyList(),
            errorMessage = null,
            isEnd = false,
            isLoading = false,
        )
        nextUrl = null
        requestGeneration++
        loadMore()
    }

    private fun loadMore() {
        if (uiState.isEnd || uiState.isLoading || uiState.errorMessage != null || loadJob?.isActive == true) return
        val generation = requestGeneration
        loadJob = viewModelScope.launch {
            try {
                uiState = uiState.copy(isLoading = true, errorMessage = null)
                val result = repository.loadTopicFeed(
                    topicId = topicId,
                    tab = uiState.selectedTab,
                    discussionSort = uiState.discussionSort,
                    ideasSort = uiState.ideasSort,
                    nextUrl = nextUrl,
                )
                if (generation != requestGeneration) return@launch
                val existingKeys = uiState.items.map { it.stableKey }.toSet()
                val newItems = result.items.filter { it.stableKey !in existingKeys }
                nextUrl = result.nextUrl
                uiState = uiState.copy(
                    items = uiState.items + newItems,
                    isEnd = result.isEnd,
                    isLoading = false,
                    errorMessage = result.error,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("TopicViewModel", "Failed to load topic feed", e)
                if (generation == requestGeneration) {
                    uiState = uiState.copy(
                        isLoading = false,
                        errorMessage = e.message ?: e::class.simpleName ?: "未知错误",
                    )
                }
            }
        }
    }

    private fun retry() {
        uiState = uiState.copy(errorMessage = null)
        loadMore()
    }

    private fun follow(following: Boolean) {
        val current = uiState.detail ?: return
        if (uiState.isFollowingChanging || current.isFollowing == following) return
        uiState = uiState.copy(isFollowingChanging = true)
        viewModelScope.launch {
            repository
                .setFollowing(topicId, following)
                .onSuccess {
                    uiState = uiState.copy(
                        detail = current.copy(
                            isFollowing = following,
                            followersCount = (current.followersCount + if (following) 1 else -1).coerceAtLeast(0),
                        ),
                        isFollowingChanging = false,
                    )
                }.onFailure {
                    uiState = uiState.copy(isFollowingChanging = false)
                    _effect.send(TopicEffect.ShowMessage("${if (following) "关注" else "取消关注"}失败：${it.message}"))
                }
        }
    }
}
