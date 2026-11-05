package com.zhihuminus.feature.follow

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuminus.core.error.AppError
import com.zhihuminus.core.error.reportFailure
import com.zhihuminus.core.error.withPrefix
import com.zhihuminus.data.FeedDisplayItem
import com.zhihuminus.data.navDestination
import com.zhihuminus.navigation.Person
import com.zhihuminus.navigation.withReadingQueueSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class FollowUiState(
    val users: List<FollowingUser> = emptyList(),
    val usersErrorMessage: AppError? = null,
    val items: List<FeedDisplayItem> = emptyList(),
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isEnd: Boolean = false,
    val errorMessage: AppError? = null,
)

class FollowViewModel(
    private val repository: FollowRepository,
) : ViewModel() {
    var uiState by mutableStateOf(FollowUiState())
        private set

    private val _effect = Channel<FollowEffect>(capacity = Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var nextUrl: String? = null
    private var loadFeedJob: Job? = null
    private var loadUsersJob: Job? = null

    init {
        loadUsers()
        loadFeeds(reset = true)
    }

    fun onEvent(event: FollowEvent) {
        when (event) {
            is FollowEvent.Refresh -> {
                loadUsers()
                loadFeeds(reset = true)
            }

            is FollowEvent.LoadMore -> loadFeeds(reset = false)

            is FollowEvent.UserClick -> {
                viewModelScope.launch {
                    _effect.send(
                        FollowEffect.Navigate(
                            Person(
                                id = event.user.id,
                                urlToken = event.user.urlToken,
                                name = event.user.name,
                                jumpTo = "动态",
                            ),
                        ),
                    )
                }
            }

            is FollowEvent.ContentClick -> {
                val destination = event.item.navDestination?.withReadingQueueSource("follow:dynamic")
                if (destination != null) {
                    viewModelScope.launch {
                        _effect.send(FollowEffect.Navigate(destination))
                    }
                } else if (event.item.content?.startsWith("http") == true) {
                    viewModelScope.launch {
                        _effect.send(FollowEffect.OpenExternalUrl(event.item.content))
                    }
                }
            }
        }
    }

    private fun loadUsers() {
        loadUsersJob?.cancel()
        loadUsersJob = viewModelScope.launch {
            try {
                val users = repository.fetchRecentFollowingUsers()
                uiState = uiState.copy(
                    users = users,
                    usersErrorMessage = null,
                )
            } catch (e: Exception) {
                val error = reportFailure("FollowViewModel", "load following users", e)
                uiState = uiState.copy(
                    usersErrorMessage = error,
                )
            }
        }
    }

    private fun loadFeeds(reset: Boolean) {
        if (loadFeedJob?.isActive == true) return
        if (!reset && (uiState.isEnd || uiState.isLoadingMore)) return

        loadFeedJob = viewModelScope.launch {
            if (reset) {
                uiState = uiState.copy(isRefreshing = true, errorMessage = null)
                nextUrl = null
            } else {
                uiState = uiState.copy(isLoadingMore = true, errorMessage = null)
            }

            try {
                val page = repository.fetchFollowFeed(nextUrl)
                val combined = if (reset) {
                    page.items
                } else {
                    val existingKeys = uiState.items.map { it.stableKey }.toSet()
                    uiState.items + page.items.filterNot { it.stableKey in existingKeys }
                }
                nextUrl = page.nextUrl
                uiState = uiState.copy(
                    items = combined,
                    isEnd = page.isEnd,
                    isRefreshing = false,
                    isLoadingMore = false,
                )
            } catch (e: Exception) {
                val error = reportFailure("FollowViewModel", "load follow feed", e)
                val message = error.withPrefix("加载关注动态").userMessage
                uiState = uiState.copy(
                    isRefreshing = false,
                    isLoadingMore = false,
                    errorMessage = error,
                )
                _effect.send(FollowEffect.ShowMessage(message, isError = true))
            }
        }
    }
}
