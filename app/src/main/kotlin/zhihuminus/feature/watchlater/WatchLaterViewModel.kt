package com.zhihuminus.feature.watchlater

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuminus.core.error.AppError
import com.zhihuminus.core.error.reportFailure
import com.zhihuminus.core.error.withPrefix
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class WatchLaterUiState(
    val items: List<WatchLaterItem> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: AppError? = null,
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
                val error = reportFailure("WatchLaterViewModel", "refresh watch later", e)
                uiState = uiState.copy(
                    isRefreshing = false,
                    isLoading = false,
                    errorMessage = error,
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
                val error = reportFailure("WatchLaterViewModel", "delete item", e)
                _effect.send(WatchLaterEffect.ShowMessage(error.withPrefix("删除").userMessage, isError = true))
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
                val error = reportFailure("WatchLaterViewModel", "clear all", e)
                _effect.send(WatchLaterEffect.ShowMessage(error.withPrefix("清除").userMessage, isError = true))
            }
        }
    }
}
