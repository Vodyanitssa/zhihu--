package com.zhihuminus.feature.notification

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

class NotificationViewModel(
    private val repository: NotificationRepository,
) : ViewModel() {
    var uiState by mutableStateOf(NotificationUiState())
        private set

    private val _effect = Channel<NotificationEffect>(capacity = Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var nextUrl: String? = null
    private var loadJob: Job? = null

    init {
        loadData(reset = true)
    }

    fun onEvent(event: NotificationEvent) {
        when (event) {
            is NotificationEvent.Refresh -> loadData(reset = true)
            is NotificationEvent.LoadMore -> loadData(reset = false)
            is NotificationEvent.MarkAllAsRead -> markAllAsRead()
            is NotificationEvent.MarkCategoryAsRead -> markCategoryAsRead(event.category)
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

                val result = repository.getNotificationOverview(if (reset) null else nextUrl)
                val newItems = if (reset) {
                    result.items
                } else {
                    val existingIds = uiState.items.mapTo(mutableSetOf()) { it.stableId }
                    uiState.items + result.items.filter { existingIds.add(it.stableId) }
                }

                nextUrl = result.nextUrl
                val unreadCounts = if (reset) result.unreadCounts else uiState.unreadCounts
                val unreadCount = unreadCounts.values.sum()
                val invitation = if (reset) result.invitation else uiState.invitation

                uiState = uiState.copy(
                    items = newItems,
                    unreadCounts = unreadCounts,
                    invitation = invitation,
                    unreadCount = unreadCount,
                    isLoading = false,
                    isRefreshing = false,
                    isLoadingMore = false,
                    isEnd = result.isEnd || result.nextUrl.isNullOrBlank(),
                    errorMessage = null,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("NotificationVM", "Failed to load notification overview", e)
                uiState = uiState.copy(
                    isLoading = false,
                    isRefreshing = false,
                    isLoadingMore = false,
                    errorMessage = "加载失败: ${friendlyErrorMessage(e)}",
                )
                if (!reset) {
                    _effect.send(NotificationEffect.ShowMessage("加载更多失败: ${friendlyErrorMessage(e)}"))
                }
            }
        }
    }

    private fun markAllAsRead() {
        viewModelScope.launch {
            try {
                val success = repository.markAllRead()
                if (success) {
                    val clearedCounts = uiState.unreadCounts.mapValues { 0 }
                    uiState = uiState.copy(
                        unreadCounts = clearedCounts,
                        unreadCount = 0,
                    )
                    _effect.send(NotificationEffect.ShowMessage("已全部标记为已读"))
                } else {
                    _effect.send(NotificationEffect.ShowMessage("标记已读失败"))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("NotificationVM", "Failed to mark all as read", e)
                _effect.send(NotificationEffect.ShowMessage("标记已读失败: ${friendlyErrorMessage(e)}"))
            }
        }
    }

    private fun markCategoryAsRead(category: NotificationCategory) {
        if ((uiState.unreadCounts[category] ?: 0) <= 0) return
        viewModelScope.launch {
            try {
                val success = repository.markCategoryRead(category.entryName)
                if (success) {
                    val updatedCounts = uiState.unreadCounts.toMutableMap().apply {
                        put(category, 0)
                    }
                    uiState = uiState.copy(
                        unreadCounts = updatedCounts,
                        unreadCount = updatedCounts.values.sum(),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("NotificationVM", "Failed to mark ${category.entryName} as read", e)
            }
        }
    }
}
