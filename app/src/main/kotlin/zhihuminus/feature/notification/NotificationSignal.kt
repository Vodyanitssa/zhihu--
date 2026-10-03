package com.zhihuminus.feature.notification

data class NotificationUiState(
    val items: List<NotificationTimelineItem> = emptyList(),
    val unreadCounts: Map<NotificationCategory, Int> = emptyMap(),
    val invitation: NotificationColumnHead? = null,
    val unreadCount: Int = 0,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isEnd: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface NotificationEvent {
    data object Refresh : NotificationEvent

    data object LoadMore : NotificationEvent

    data object MarkAllAsRead : NotificationEvent

    data class MarkCategoryAsRead(
        val category: NotificationCategory,
    ) : NotificationEvent
}

sealed interface NotificationEffect {
    data class ShowMessage(
        val message: String,
    ) : NotificationEffect
}
