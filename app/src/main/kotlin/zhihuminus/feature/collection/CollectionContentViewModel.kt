package com.zhihuminus.feature.collection

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuminus.data.FeedDisplayItem
import com.zhihuminus.util.Log
import com.zhihuminus.util.friendlyErrorMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class CollectionContentUiState(
    val collectionId: String = "",
    val collection: Collection? = null,
    val items: List<FeedDisplayItem> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isEnd: Boolean = false,
    val errorMessage: String? = null,
)

class CollectionContentViewModel(
    val collectionId: String,
    private val repository: CollectionRepository,
) : ViewModel() {
    var uiState by mutableStateOf(CollectionContentUiState(collectionId = collectionId))
        private set

    private val _effect = Channel<CollectionContentEffect>(capacity = Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var nextUrl: String? = null
    private var loadJob: Job? = null

    init {
        loadData(reset = true)
    }

    fun onEvent(event: CollectionContentEvent) {
        when (event) {
            is CollectionContentEvent.Refresh -> loadData(reset = true)
            is CollectionContentEvent.LoadMore -> loadData(reset = false)
        }
    }

    private fun loadData(reset: Boolean) {
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

                val currentCollection = if (reset || uiState.collection == null) {
                    try {
                        repository.getCollection(collectionId)
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        Log.e("CollectionContentVM", "Failed to fetch collection details $collectionId", e)
                        uiState.collection
                    }
                } else {
                    uiState.collection
                }

                val result = repository.getCollectionItems(
                    collectionId = collectionId,
                    nextUrl = if (reset) null else nextUrl,
                )

                val newItems = if (reset) {
                    result.items
                } else {
                    val existingKeys = uiState.items.map { it.stableKey }.toSet()
                    uiState.items + result.items.filter { it.stableKey !in existingKeys }
                }

                nextUrl = result.nextUrl
                uiState = uiState.copy(
                    collection = currentCollection,
                    items = newItems,
                    isLoading = false,
                    isRefreshing = false,
                    isLoadingMore = false,
                    isEnd = result.isEnd || result.nextUrl.isNullOrBlank(),
                    errorMessage = null,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("CollectionContentVM", "Failed to load collection items $collectionId", e)
                uiState = uiState.copy(
                    isLoading = false,
                    isRefreshing = false,
                    isLoadingMore = false,
                    errorMessage = "加载失败: ${friendlyErrorMessage(e)}",
                )
                if (!reset) {
                    _effect.send(CollectionContentEffect.ShowMessage("加载更多失败: ${friendlyErrorMessage(e)}"))
                }
            }
        }
    }
}
