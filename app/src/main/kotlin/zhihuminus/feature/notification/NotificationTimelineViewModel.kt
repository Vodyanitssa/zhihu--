package com.zhihuminus.feature.notification

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

class NotificationTimelineViewModel(
    val entryName: String,
    val title: String,
    private val repository: NotificationRepository,
    private val settingsStore: NotificationSettingsStore,
) : ViewModel() {
    var uiState by mutableStateOf(NotificationTimelineUiState(entryName = entryName, title = title))
        private set

    private val _effect = Channel<NotificationTimelineEffect>(capacity = Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var nextUrl: String? = null
    private var loadJob: Job? = null
    private var markedAsRead = false

    init {
        loadData(reset = true)
    }

    fun onEvent(event: NotificationTimelineEvent) {
        when (event) {
            is NotificationTimelineEvent.Refresh -> {
                markedAsRead = false
                loadData(reset = true)
            }
            is NotificationTimelineEvent.LoadMore -> loadData(reset = false)
            is NotificationTimelineEvent.MarkAsRead -> markAsRead()
        }
    }

    private fun shouldShowNotification(notification: NotificationTimelineItem): Boolean {
        if (notification.type == "empty") return true
        if (entryName == "invite") return true
        val content = notification.content ?: return true
        val verb = listOf(content.subTitle, content.title, content.text)
            .firstOrNull { it.isNotBlank() }
            .orEmpty()
        val type = matchNotificationType(verb)
        return type == null || settingsStore.getDisplayInAppEnabled(type)
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

                val result = repository.getNotificationTimeline(entryName, if (reset) null else nextUrl)
                val filteredItems = result.items.filter { shouldShowNotification(it) }

                val newItems = if (reset) {
                    filteredItems
                } else {
                    val existingIds = uiState.items.mapTo(mutableSetOf()) { it.stableId }
                    uiState.items + filteredItems.filter { existingIds.add(it.stableId) }
                }

                nextUrl = result.nextUrl
                uiState = uiState.copy(
                    items = newItems,
                    isLoading = false,
                    isRefreshing = false,
                    isLoadingMore = false,
                    isEnd = result.isEnd || result.nextUrl.isNullOrBlank(),
                    errorMessage = null,
                )

                if (!markedAsRead && settingsStore.getAutoMarkAsReadEnabled()) {
                    val success = repository.markCategoryRead(entryName)
                    if (success) {
                        markedAsRead = true
                        uiState = uiState.copy(
                            items = uiState.items.map { it.copy(isRead = true) },
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("NotificationTimelineVM", "Failed to load timeline for $entryName", e)
                uiState = uiState.copy(
                    isLoading = false,
                    isRefreshing = false,
                    isLoadingMore = false,
                    errorMessage = "加载失败: ${friendlyErrorMessage(e)}",
                )
                if (!reset) {
                    _effect.send(NotificationTimelineEffect.ShowMessage("加载更多失败: ${friendlyErrorMessage(e)}"))
                }
            }
        }
    }

    private fun markAsRead() {
        viewModelScope.launch {
            try {
                val success = repository.markCategoryRead(entryName)
                if (success) {
                    markedAsRead = true
                    uiState = uiState.copy(
                        items = uiState.items.map { it.copy(isRead = true) },
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("NotificationTimelineVM", "Failed to mark $entryName as read", e)
            }
        }
    }
}
