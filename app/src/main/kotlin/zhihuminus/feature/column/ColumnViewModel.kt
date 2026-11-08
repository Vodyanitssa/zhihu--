package com.zhihuminus.feature.column

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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class ColumnUiState(
    val columnId: String = "",
    val detail: ColumnDetail? = null,
    val detailErrorMessage: AppError? = null,
    val isFollowingChanging: Boolean = false,
    val isAuthorFollowingChanging: Boolean = false,
    val articles: List<FeedDisplayItem> = emptyList(),
    val totals: Int = 0,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isEnd: Boolean = false,
    val errorMessage: AppError? = null,
)

class ColumnViewModel(
    private val columnId: String,
    private val repository: ColumnRepository,
) : ViewModel() {
    var uiState by mutableStateOf(ColumnUiState(columnId = columnId))
        private set

    private val _effect = Channel<ColumnEffect>(capacity = Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var loadJob: Job? = null
    private var nextUrl: String? = null

    init {
        loadDetail()
        loadArticles(reset = true)
    }

    fun onEvent(event: ColumnEvent) {
        when (event) {
            is ColumnEvent.Refresh -> {
                loadDetail()
                loadArticles(reset = true)
            }

            is ColumnEvent.LoadMore -> loadArticles(reset = false)
            is ColumnEvent.RetryDetail -> loadDetail()
            is ColumnEvent.Follow -> follow(event.following)
            is ColumnEvent.FollowAuthor -> followAuthor(event.following)
            is ColumnEvent.ContentClick -> {
                val destination = event.item.navDestination
                if (destination != null) {
                    viewModelScope.launch {
                        _effect.send(ColumnEffect.Navigate(destination))
                    }
                }
            }

            is ColumnEvent.AuthorClick -> {
                if (event.id.isNotBlank() || event.urlToken.isNotBlank()) {
                    viewModelScope.launch {
                        _effect.send(
                            ColumnEffect.Navigate(
                                Person(
                                    id = event.id,
                                    urlToken = event.urlToken,
                                    name = event.name,
                                ),
                            ),
                        )
                    }
                }
            }
        }
    }

    private fun loadDetail() {
        viewModelScope.launch {
            try {
                val detail = repository.getColumnDetail(columnId)
                uiState = uiState.copy(detail = detail, detailErrorMessage = null)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val error = reportFailure("ColumnViewModel", "load column detail", e)
                uiState = uiState.copy(detailErrorMessage = error)
            }
        }
    }

    private fun follow(following: Boolean) {
        val current = uiState.detail ?: return
        if (uiState.isFollowingChanging || current.isFollowing == following) return
        uiState = uiState.copy(isFollowingChanging = true)
        viewModelScope.launch {
            try {
                repository.setFollowing(columnId, following)
                uiState = uiState.copy(
                    detail = current.copy(
                        isFollowing = following,
                        followerCount = (current.followerCount + if (following) 1 else -1).coerceAtLeast(0),
                    ),
                    isFollowingChanging = false,
                )
                sendEffect(ColumnEffect.ShowMessage(if (following) "已关注专栏" else "已取消关注专栏"))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val error = reportFailure("ColumnViewModel", "toggle follow", e)
                uiState = uiState.copy(isFollowingChanging = false)
                sendEffect(
                    ColumnEffect.ShowMessage(
                        error.withPrefix(if (following) "关注" else "取消关注").userMessage,
                        isError = true,
                    ),
                )
            }
        }
    }

    private fun followAuthor(following: Boolean) {
        val current = uiState.detail ?: return
        val author = current.author ?: return
        val token = author.urlToken.takeIf { it.isNotBlank() } ?: author.id
        if (token.isBlank() || uiState.isAuthorFollowingChanging || author.isFollowing == following) return

        val previousIsFollowing = author.isFollowing
        uiState = uiState.copy(
            detail = current.copy(author = author.copy(isFollowing = following)),
            isAuthorFollowingChanging = true,
        )
        viewModelScope.launch {
            try {
                repository.setAuthorFollowing(token, following)
                uiState = uiState.copy(isAuthorFollowingChanging = false)
                sendEffect(ColumnEffect.ShowMessage(if (following) "已关注作者" else "已取消关注作者"))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val error = reportFailure(
                    "ColumnViewModel",
                    "follow column author",
                    e,
                    userAction = if (following) "关注" else "取消关注",
                )
                val latest = uiState.detail ?: current
                uiState = uiState.copy(
                    detail = latest.copy(author = latest.author?.copy(isFollowing = previousIsFollowing)),
                    isAuthorFollowingChanging = false,
                )
                sendEffect(ColumnEffect.ShowMessage(error.userMessage, isError = true))
            }
        }
    }

    private fun loadArticles(reset: Boolean) {
        if (reset) {
            nextUrl = null
        }
        if (uiState.isLoadingMore && !reset) return

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                if (reset) {
                    uiState = uiState.copy(isRefreshing = true, articles = emptyList())
                } else {
                    uiState = uiState.copy(isLoadingMore = true)
                }

                val result = repository.getColumnArticles(
                    columnId = columnId,
                    nextUrl = if (reset) null else nextUrl,
                )

                val newArticles = if (reset) {
                    result.articles
                } else {
                    uiState.articles + result.articles
                }

                nextUrl = result.nextUrl
                uiState = uiState.copy(
                    articles = newArticles,
                    totals = result.totals,
                    isRefreshing = false,
                    isLoadingMore = false,
                    isEnd = result.isEnd,
                    errorMessage = null,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val error = reportFailure("ColumnViewModel", "load column articles", e)
                uiState = uiState.copy(
                    isRefreshing = false,
                    isLoadingMore = false,
                    errorMessage = error,
                )
                sendEffect(ColumnEffect.ShowMessage(error.withPrefix("加载").userMessage, isError = true))
            }
        }
    }

    private fun sendEffect(effect: ColumnEffect) {
        viewModelScope.launch { _effect.send(effect) }
    }
}
