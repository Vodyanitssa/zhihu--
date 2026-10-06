package com.zhihuminus.feature.history

import com.zhihuminus.navigation.NavDestination

sealed interface HistoryEvent {
    data object Refresh : HistoryEvent

    data object LoadMore : HistoryEvent

    data class DeleteItem(
        val item: HistoryItem,
    ) : HistoryEvent

    data object ClearAll : HistoryEvent

    data class ContentClick(
        val item: HistoryItem,
    ) : HistoryEvent
}

sealed interface HistoryEffect {
    data class ShowMessage(
        val message: String,
    ) : HistoryEffect

    data class Navigate(
        val destination: NavDestination,
    ) : HistoryEffect
}
