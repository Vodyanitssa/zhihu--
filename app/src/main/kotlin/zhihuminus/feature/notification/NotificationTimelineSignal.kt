package com.zhihuminus.feature.notification

import com.zhihuminus.core.error.AppError

data class NotificationTimelineUiState(
    val entryName: String = "",
    val title: String = "",
    val items: List<NotificationTimelineItem> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isEnd: Boolean = false,
    val errorMessage: AppError? = null,
)

sealed interface NotificationTimelineEvent {
    data object Refresh : NotificationTimelineEvent

    data object LoadMore : NotificationTimelineEvent

    data object MarkAsRead : NotificationTimelineEvent
}

sealed interface NotificationTimelineEffect {
    data class ShowMessage(
        val message: String,
    ) : NotificationTimelineEffect
}
