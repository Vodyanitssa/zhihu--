package com.zhihuminus.feature.collection

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuminus.core.error.AppError
import com.zhihuminus.core.error.reportFailure
import com.zhihuminus.core.error.withPrefix
import com.zhihuminus.data.FeedDisplayItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class CollectionContentUiState(
    val collectionId: String = "",
    val collection: Collection? = null,
    val detailErrorMessage: AppError? = null,
    val items: List<FeedDisplayItem> = emptyList(),
    val totalItems: Int? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isEnd: Boolean = false,
    val errorMessage: AppError? = null,
    val itemPendingRemoval: PendingCollectionItemRemoval? = null,
    val isRemovingItem: Boolean = false,
    val removeError: String? = null,
    val showDeleteDialog: Boolean = false,
    val isDeleting: Boolean = false,
    val deleteError: String? = null,
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
    private var detailJob: Job? = null

    init {
        loadDetail()
        loadItems(reset = true)
    }

    fun onEvent(event: CollectionContentEvent) {
        when (event) {
            is CollectionContentEvent.Refresh -> {
                loadDetail()
                loadItems(reset = true)
            }

            is CollectionContentEvent.LoadMore -> loadItems(reset = false)
            is CollectionContentEvent.RetryDetail -> loadDetail()
            is CollectionContentEvent.RequestRemoveItem -> uiState = uiState.copy(
                itemPendingRemoval = event.target,
                removeError = null,
            )

            is CollectionContentEvent.DismissRemoveItemDialog -> {
                if (!uiState.isRemovingItem) {
                    uiState = uiState.copy(itemPendingRemoval = null, removeError = null)
                }
            }

            is CollectionContentEvent.ConfirmRemoveItem -> removePendingItem()
            is CollectionContentEvent.RequestDelete -> requestDelete()
            is CollectionContentEvent.DismissDeleteDialog -> {
                if (!uiState.isDeleting) {
                    uiState = uiState.copy(showDeleteDialog = false, deleteError = null)
                }
            }

            is CollectionContentEvent.ConfirmDelete -> deleteCollection()
        }
    }

    private fun loadDetail() {
        detailJob?.cancel()
        detailJob = viewModelScope.launch {
            uiState = uiState.copy(detailErrorMessage = null)
            try {
                val collection = repository.getCollection(collectionId)
                val currentTotal = uiState.totalItems
                uiState = uiState.copy(
                    collection = collection,
                    detailErrorMessage = null,
                    totalItems = currentTotal?.takeIf { it > 0 }
                        ?: collection.itemCount.takeIf { it > 0 }
                        ?: currentTotal,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val error = reportFailure("CollectionContentViewModel", "fetch collection details $collectionId", e)
                uiState = uiState.copy(detailErrorMessage = error)
            }
        }
    }

    private fun loadItems(reset: Boolean) {
        if (reset) {
            nextUrl = null
        }
        if (uiState.isLoadingMore && !reset) return
        if (uiState.isEnd && !reset) return

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (reset) {
                uiState = uiState.copy(
                    isRefreshing = uiState.items.isNotEmpty(),
                    isLoading = uiState.items.isEmpty(),
                    errorMessage = null,
                )
            } else {
                uiState = uiState.copy(isLoadingMore = true)
            }

            try {
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
                val currentTotal = uiState.totalItems
                val resolvedTotal = result.totals.takeIf { it > 0 }
                    ?: currentTotal?.takeIf { it > 0 }
                    ?: uiState.collection?.itemCount?.takeIf { it > 0 }
                    ?: newItems.size

                uiState = uiState.copy(
                    items = newItems,
                    totalItems = resolvedTotal,
                    isLoading = false,
                    isRefreshing = false,
                    isLoadingMore = false,
                    isEnd = result.isEnd || result.nextUrl.isNullOrBlank(),
                    errorMessage = null,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val error = reportFailure("CollectionContentViewModel", "load collection items $collectionId", e)
                val hasItems = uiState.items.isNotEmpty()
                uiState = uiState.copy(
                    isLoading = false,
                    isRefreshing = false,
                    isLoadingMore = false,
                    errorMessage = if (hasItems) null else error,
                )
                if (hasItems) {
                    _effect.send(
                        CollectionContentEffect.ShowMessage(
                            error.withPrefix(if (reset) "刷新" else "加载更多").userMessage,
                            isError = true,
                        ),
                    )
                }
            }
        }
    }

    private fun removePendingItem() {
        val pending = uiState.itemPendingRemoval ?: return
        if (uiState.isRemovingItem) return

        uiState = uiState.copy(isRemovingItem = true, removeError = null)
        viewModelScope.launch {
            try {
                repository.removeFromCollection(pending.type, pending.id, collectionId)
                val remaining = uiState.items.filterNot { it.stableKey == pending.stableKey }
                val currentTotal = uiState.totalItems
                    ?: uiState.collection?.itemCount?.takeIf { it > 0 }
                    ?: uiState.items.size
                uiState = uiState.copy(
                    items = remaining,
                    totalItems = (currentTotal - 1).coerceAtLeast(0),
                    isEnd = uiState.isEnd || (remaining.isEmpty() && nextUrl == null),
                    itemPendingRemoval = null,
                    isRemovingItem = false,
                    removeError = null,
                )
                _effect.send(CollectionContentEffect.ShowMessage("已移出收藏夹"))
                if (remaining.isEmpty() && nextUrl != null && !uiState.isEnd) {
                    loadItems(reset = false)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val error = reportFailure(
                    "CollectionContentViewModel",
                    "remove collection item ${pending.type}/${pending.id} from $collectionId",
                    e,
                    userAction = "移出",
                )
                uiState = uiState.copy(isRemovingItem = false, removeError = error.userMessage)
            }
        }
    }

    private fun requestDelete() {
        val collection = uiState.collection ?: return
        if (collection.isDefault) {
            _effect.trySend(CollectionContentEffect.ShowMessage("默认收藏夹不能删除", isError = true))
            return
        }
        uiState = uiState.copy(showDeleteDialog = true, deleteError = null)
    }

    private fun deleteCollection() {
        if (!uiState.showDeleteDialog || uiState.isDeleting) return

        uiState = uiState.copy(isDeleting = true, deleteError = null)
        viewModelScope.launch {
            try {
                repository.deleteCollection(collectionId)
                uiState = uiState.copy(isDeleting = false, showDeleteDialog = false)
                _effect.send(CollectionContentEffect.ShowMessage("收藏夹已删除"))
                _effect.send(CollectionContentEffect.NavigateBack)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val error = reportFailure(
                    "CollectionContentViewModel",
                    "delete collection $collectionId",
                    e,
                    userAction = "删除",
                )
                uiState = uiState.copy(isDeleting = false, deleteError = error.userMessage)
            }
        }
    }
}
