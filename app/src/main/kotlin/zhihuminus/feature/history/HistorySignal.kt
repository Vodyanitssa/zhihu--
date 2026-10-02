package com.zhihuminus.feature.history

sealed interface HistoryEvent {
    data object Refresh : HistoryEvent

    data object LoadMore : HistoryEvent

    data class DeleteItem(
        val item: HistoryItem,
    ) : HistoryEvent

    data object ClearAll : HistoryEvent
}

sealed interface HistoryEffect {
    data class ShowMessage(
        val message: String,
    ) : HistoryEffect
}
