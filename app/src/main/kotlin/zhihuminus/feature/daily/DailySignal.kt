package com.zhihuminus.feature.daily

sealed interface DailyEvent {
    data object Refresh : DailyEvent

    data object LoadMore : DailyEvent

    data class SelectDate(
        val date: String,
    ) : DailyEvent
}

sealed interface DailyEffect {
    data class ShowMessage(
        val message: String,
    ) : DailyEffect
}
