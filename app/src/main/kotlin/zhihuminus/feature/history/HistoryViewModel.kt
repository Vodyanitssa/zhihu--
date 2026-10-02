package com.zhihuminus.feature.history

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuminus.util.Log
import com.zhihuminus.util.friendlyErrorMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class HistoryUiState(
    val items: List<HistoryItem> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isEnd: Boolean = false,
    val errorMessage: String? = null,
)

class HistoryViewModel(
    private val repository: HistoryRepository,
) : ViewModel() {
    var uiState by mutableStateOf(HistoryUiState())
        private set

    private val _effect = Channel<HistoryEffect>(capacity = Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var nextUrl: String? = null
    private var loadJob: Job? = null

    init {
        loadHistory(reset = true)
    }

    fun onEvent(event: HistoryEvent) {
        when (event) {
            is HistoryEvent.Refresh -> loadHistory(reset = true)
            is HistoryEvent.LoadMore -> loadHistory(reset = false)
            is HistoryEvent.DeleteItem -> deleteItem(event.item)
            is HistoryEvent.ClearAll -> clearAll()
        }
    }

    private fun loadHistory(reset: Boolean) {
        if (reset) {
            nextUrl = null
        }
        if (uiState.isLoadingMore && !reset) return
        if (uiState.isEnd && !reset) return

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                if (reset) {
                    uiState = uiState.copy(
                        isRefreshing = uiState.items.isNotEmpty(),
                        isLoading = uiState.items.isEmpty(),
                        errorMessage = null,
                    )
                } else {
                    uiState = uiState.copy(isLoadingMore = true)
                }

                val result = repository.fetchHistory(nextUrl = if (reset) null else nextUrl)
                val newItems = if (reset) {
                    result.items
                } else {
                    val existingKeys = uiState.items.map { "${it.contentType}:${it.contentToken}" }.toSet()
                    uiState.items + result.items.filter { "${it.contentType}:${it.contentToken}" !in existingKeys }
                }

                nextUrl = result.nextUrl
                uiState = uiState.copy(
                    items = newItems,
                    isLoading = false,
                    isRefreshing = false,
                    isLoadingMore = false,
                    isEnd = result.isEnd || result.nextUrl == null,
                    errorMessage = null,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("HistoryViewModel", "Failed to load history", e)
                uiState = uiState.copy(
                    isLoading = false,
                    isRefreshing = false,
                    isLoadingMore = false,
                    errorMessage = "加载失败: ${friendlyErrorMessage(e)}",
                )
                if (!reset) {
                    _effect.send(HistoryEffect.ShowMessage("加载更多失败: ${friendlyErrorMessage(e)}"))
                }
            }
        }
    }

    private fun deleteItem(item: HistoryItem) {
        viewModelScope.launch {
            try {
                repository.deleteItem(HistoryDeletePair(item.contentToken, item.contentType))
                uiState = uiState.copy(
                    items = uiState.items.filterNot {
                        it.contentToken == item.contentToken && it.contentType == item.contentType
                    },
                )
                _effect.send(HistoryEffect.ShowMessage("已删除"))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("HistoryViewModel", "Failed to delete history item", e)
                _effect.send(HistoryEffect.ShowMessage("删除失败: ${friendlyErrorMessage(e)}"))
            }
        }
    }

    private fun clearAll() {
        viewModelScope.launch {
            try {
                repository.clearAll()
                uiState = uiState.copy(items = emptyList(), isEnd = true)
                _effect.send(HistoryEffect.ShowMessage("已清除所有历史记录"))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("HistoryViewModel", "Failed to clear history", e)
                _effect.send(HistoryEffect.ShowMessage("清除失败: ${friendlyErrorMessage(e)}"))
            }
        }
    }
}
