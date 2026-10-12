package com.zhihuminus.feature.watchlater

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuminus.core.util.Log
import com.zhihuminus.core.util.friendlyErrorMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class WatchLaterUiState(
    val items: List<WatchLaterItem> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
)

class WatchLaterViewModel(
    private val repository: WatchLaterRepository,
) : ViewModel() {
    var uiState by mutableStateOf(WatchLaterUiState())
        private set

    private val _effect = Channel<WatchLaterEffect>(capacity = Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        observeItems()
    }

    private fun observeItems() {
        viewModelScope.launch {
            repository.itemsFlow.collect { list ->
                uiState = uiState.copy(
                    items = list,
                    isLoading = false,
                    isRefreshing = false,
                    errorMessage = null,
                )
            }
        }
    }

    fun onEvent(event: WatchLaterEvent) {
        when (event) {
            is WatchLaterEvent.Refresh -> refresh()
            is WatchLaterEvent.DeleteItem -> deleteItem(event.item)
            is WatchLaterEvent.ClearAll -> clearAll()
            is WatchLaterEvent.ContentClick -> {
                viewModelScope.launch {
                    _effect.send(WatchLaterEffect.Navigate(event.item.toDestination()))
                }
            }
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            try {
                uiState = uiState.copy(isRefreshing = true, errorMessage = null)
                val items = repository.getItems()
                uiState = uiState.copy(
                    items = items,
                    isRefreshing = false,
                    isLoading = false,
                    errorMessage = null,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("WatchLaterViewModel", "Failed to refresh watch later", e)
                uiState = uiState.copy(
                    isRefreshing = false,
                    isLoading = false,
                    errorMessage = "刷新失败: ${friendlyErrorMessage(e)}",
                )
            }
        }
    }

    private fun deleteItem(item: WatchLaterItem) {
        viewModelScope.launch {
            try {
                repository.deleteItem(item.id, item.type)
                _effect.send(WatchLaterEffect.ShowMessage("已从稍后再看中删除"))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("WatchLaterViewModel", "Failed to delete item", e)
                _effect.send(WatchLaterEffect.ShowMessage("删除失败: ${friendlyErrorMessage(e)}"))
            }
        }
    }

    private fun clearAll() {
        viewModelScope.launch {
            try {
                repository.clearAll()
                _effect.send(WatchLaterEffect.ShowMessage("已清空稍后再看列表"))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("WatchLaterViewModel", "Failed to clear all", e)
                _effect.send(WatchLaterEffect.ShowMessage("清除失败: ${friendlyErrorMessage(e)}"))
            }
        }
    }
}
