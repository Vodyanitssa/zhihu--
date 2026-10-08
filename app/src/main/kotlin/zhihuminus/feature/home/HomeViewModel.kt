package com.zhihuminus.feature.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuminus.core.util.Log
import com.zhihuminus.core.util.friendlyErrorMessage
import com.zhihuminus.data.FeedDisplayItem
import com.zhihuminus.data.navDestination
import com.zhihuminus.data.zhihu.dto.AnswerTargetDto
import com.zhihuminus.data.zhihu.dto.ArticleTargetDto
import com.zhihuminus.data.zhihu.dto.PinTargetDto
import com.zhihuminus.feature.post.PostType
import com.zhihuminus.navigation.Account
import com.zhihuminus.navigation.PostDestination
import com.zhihuminus.navigation.Search
import com.zhihuminus.navigation.withReadingQueueSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val items: List<FeedDisplayItem> = emptyList(),
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isEnd: Boolean = false,
    val unreadCount: Int = 0,
    val errorMessage: String? = null,
)

class HomeViewModel(
    private val repository: HomeRepository,
    private val autoRefreshOnStartup: Boolean = true,
) : ViewModel() {
    var uiState by mutableStateOf(HomeUiState())
        private set

    private val _effect = Channel<HomeEffect>(capacity = Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var nextUrl: String? = null
    private var loadJob: Job? = null
    private val reportedTouchedItems = hashSetOf<Pair<String, String>>()

    init {
        initializeData()
    }

    private fun initializeData() {
        viewModelScope.launch {
            if (!autoRefreshOnStartup) {
                val cached = repository.loadStartupSnapshot()
                if (cached.isNotEmpty() && uiState.items.isEmpty()) {
                    uiState = uiState.copy(items = cached)
                    return@launch
                }
            }
            loadFeeds(reset = true)
        }
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.Refresh -> loadFeeds(reset = true)
            is HomeEvent.LoadMore -> loadFeeds(reset = false)
            is HomeEvent.ContentClick -> handleContentClick(event.item)
            is HomeEvent.SearchClick -> {
                viewModelScope.launch {
                    _effect.send(HomeEffect.Navigate(Search(query = "")))
                }
            }
            is HomeEvent.AvatarClick -> {
                viewModelScope.launch {
                    _effect.send(HomeEffect.Navigate(Account))
                }
            }
            is HomeEvent.RequestLogin -> {}
            is HomeEvent.UpdateUnreadCount -> {
                uiState = uiState.copy(unreadCount = event.count)
            }
        }
    }

    private fun handleContentClick(item: FeedDisplayItem) {
        resolveContentPayload(item)?.let { (type, id) ->
            viewModelScope.launch {
                repository.reportContentRead(type, id)
            }
        }
        val destination = item.navDestination?.withReadingQueueSource("home:WEB")
        viewModelScope.launch {
            if (destination != null) {
                _effect.send(HomeEffect.Navigate(destination))
            } else if (item.content?.startsWith("http") == true) {
                _effect.send(HomeEffect.OpenExternalUrl(item.content))
            } else {
                _effect.send(HomeEffect.ShowMessage("暂不支持打开该内容"))
            }
        }
    }

    private fun loadFeeds(reset: Boolean) {
        if (reset) {
            nextUrl = null
        } else if (uiState.isLoadingMore || uiState.isRefreshing || uiState.isEnd) {
            return
        }

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (reset) {
                reportedTouchedItems.clear()
                uiState = uiState.copy(isRefreshing = true, errorMessage = null)
            } else {
                uiState = uiState.copy(isLoadingMore = true, errorMessage = null)
            }

            triggerTouchReport()

            try {
                val page = repository.fetchRecommendFeed(nextUrl)
                val currentItems = if (reset) emptyList() else uiState.items
                val existingKeys = currentItems.map { it.stableKey }.toSet()
                val incomingDistinct = page.items.filter { it.stableKey !in existingKeys }
                val updatedItems = currentItems + incomingDistinct

                nextUrl = page.nextUrl
                uiState = uiState.copy(
                    items = updatedItems,
                    isRefreshing = false,
                    isLoadingMore = false,
                    isEnd = page.isEnd,
                )

                if (updatedItems.isNotEmpty()) {
                    repository.saveStartupSnapshot(updatedItems)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("HomeViewModel", "Failed to load recommend feeds", e)
                val friendlyMessage = friendlyErrorMessage(e)
                uiState = uiState.copy(
                    isRefreshing = false,
                    isLoadingMore = false,
                    errorMessage = friendlyMessage,
                )
                _effect.send(HomeEffect.ShowMessage(friendlyMessage))
            }
        }
    }

    private suspend fun triggerTouchReport() {
        val currentTouchItems = uiState.items.mapNotNull(::resolveContentPayload)
        val untouched = currentTouchItems.filter { it !in reportedTouchedItems }
        if (untouched.isNotEmpty()) {
            repository.reportContentTouch(untouched)
            reportedTouchedItems.addAll(untouched)
        }
    }

    private fun resolveContentPayload(item: FeedDisplayItem): Pair<String, String>? {
        val dest = item.navDestination
        if (dest is PostDestination) {
            val type = when (dest.type) {
                PostType.Answer -> "answer"
                PostType.Article -> "article"
                PostType.Pin -> "pin"
            }
            return type to dest.id.toString()
        }
        return when (val target = item.feed?.target) {
            is AnswerTargetDto -> "answer" to target.id.toString()
            is ArticleTargetDto -> "article" to target.id.toString()
            is PinTargetDto -> "pin" to target.id.toString()
            else -> null
        }
    }
}
