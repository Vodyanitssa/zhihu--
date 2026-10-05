package com.zhihuminus.feature.collection

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuminus.core.util.Log
import com.zhihuminus.core.util.friendlyErrorMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class CollectionUiState(
    val collections: List<Collection> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isEnd: Boolean = false,
    val errorMessage: String? = null,
    val showCreateDialog: Boolean = false,
    val isCreating: Boolean = false,
    val createError: String? = null,
    val collectionPendingDeletion: Collection? = null,
    val isDeleting: Boolean = false,
    val deleteError: String? = null,
)

class CollectionViewModel(
    private val urlToken: String,
    private val repository: CollectionRepository,
) : ViewModel() {
    var uiState by mutableStateOf(CollectionUiState())
        private set

    private val _effect = Channel<CollectionEffect>(capacity = Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var nextUrl: String? = null
    private var loadJob: Job? = null

    init {
        loadCollections(reset = true)
    }

    fun onEvent(event: CollectionEvent) {
        when (event) {
            is CollectionEvent.Refresh -> loadCollections(reset = true)
            is CollectionEvent.LoadMore -> loadCollections(reset = false)
            is CollectionEvent.OpenCreateDialog -> {
                uiState = uiState.copy(showCreateDialog = true, createError = null)
            }
            is CollectionEvent.DismissCreateDialog -> {
                if (!uiState.isCreating) {
                    uiState = uiState.copy(showCreateDialog = false, createError = null)
                }
            }
            is CollectionEvent.CreateCollection -> {
                createCollection(event.title, event.description, event.isPublic)
            }
            is CollectionEvent.RequestDelete -> {
                uiState = uiState.copy(collectionPendingDeletion = event.collection, deleteError = null)
            }
            is CollectionEvent.DismissDeleteDialog -> {
                if (!uiState.isDeleting) {
                    uiState = uiState.copy(collectionPendingDeletion = null, deleteError = null)
                }
            }
            is CollectionEvent.ConfirmDelete -> {
                deleteCollection()
            }
        }
    }

    private fun loadCollections(reset: Boolean) {
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
                        isRefreshing = uiState.collections.isNotEmpty(),
                        isLoading = uiState.collections.isEmpty(),
                        errorMessage = null,
                    )
                } else {
                    uiState = uiState.copy(isLoadingMore = true)
                }

                val result = repository.getUserCollections(urlToken = urlToken, nextUrl = if (reset) null else nextUrl)
                val newCollections = if (reset) {
                    result.items
                } else {
                    val existingIds = uiState.collections.map { it.id }.toSet()
                    uiState.collections + result.items.filter { it.id !in existingIds }
                }

                nextUrl = result.nextUrl
                uiState = uiState.copy(
                    collections = newCollections,
                    isLoading = false,
                    isRefreshing = false,
                    isLoadingMore = false,
                    isEnd = result.isEnd || result.nextUrl.isNullOrBlank(),
                    errorMessage = null,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("CollectionViewModel", "Failed to load collections", e)
                uiState = uiState.copy(
                    isLoading = false,
                    isRefreshing = false,
                    isLoadingMore = false,
                    errorMessage = "加载失败: ${friendlyErrorMessage(e)}",
                )
                if (!reset) {
                    _effect.send(CollectionEffect.ShowMessage("加载更多失败: ${friendlyErrorMessage(e)}"))
                }
            }
        }
    }

    private fun createCollection(title: String, description: String, isPublic: Boolean) {
        if (uiState.isCreating) return
        viewModelScope.launch {
            uiState = uiState.copy(isCreating = true, createError = null)
            try {
                repository.createCollection(title, description, isPublic)
                uiState = uiState.copy(isCreating = false, showCreateDialog = false, createError = null)
                _effect.send(CollectionEffect.ShowMessage("收藏夹已创建"))
                loadCollections(reset = true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("CollectionViewModel", "Failed to create collection", e)
                uiState = uiState.copy(
                    isCreating = false,
                    createError = friendlyErrorMessage(e),
                )
            }
        }
    }

    private fun deleteCollection() {
        val collection = uiState.collectionPendingDeletion ?: return
        if (uiState.isDeleting) return
        if (collection.isDefault) {
            uiState = uiState.copy(deleteError = "默认收藏夹不能删除")
            return
        }

        viewModelScope.launch {
            uiState = uiState.copy(isDeleting = true, deleteError = null)
            try {
                repository.deleteCollection(collection.id)
                uiState = uiState.copy(
                    isDeleting = false,
                    collectionPendingDeletion = null,
                    deleteError = null,
                    collections = uiState.collections.filterNot { it.id == collection.id },
                )
                _effect.send(CollectionEffect.ShowMessage("收藏夹已删除"))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("CollectionViewModel", "Failed to delete collection", e)
                uiState = uiState.copy(
                    isDeleting = false,
                    deleteError = friendlyErrorMessage(e),
                )
            }
        }
    }
}
