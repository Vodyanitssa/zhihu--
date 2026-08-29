package com.zhihuminus.feature.column

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

data class ColumnUiState(
    val columnId: String = "",
    val articles: List<FeedDisplayItem> = emptyList(),
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isEnd: Boolean = false,
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
        loadArticles(reset = true)
    }

    fun onEvent(event: ColumnEvent) {
        when (event) {
            is ColumnEvent.Refresh -> loadArticles(reset = true)
            is ColumnEvent.LoadMore -> loadArticles(reset = false)
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
                    isRefreshing = false,
                    isLoadingMore = false,
                    isEnd = result.isEnd,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("ColumnViewModel", "Failed to load column articles", e)
                uiState = uiState.copy(
                    isRefreshing = false,
                    isLoadingMore = false,
                )
                sendEffect(ColumnEffect.ShowMessage("加载失败: ${e.message}"))
            }
        }
    }

    private fun sendEffect(effect: ColumnEffect) {
        viewModelScope.launch { _effect.send(effect) }
    }
}
