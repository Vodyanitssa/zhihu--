package com.zhihuminus.feature.watchlater

import com.zhihuminus.navigation.NavDestination

sealed interface WatchLaterEvent {
    data object Refresh : WatchLaterEvent

    data class DeleteItem(
        val item: WatchLaterItem,
    ) : WatchLaterEvent

    data object ClearAll : WatchLaterEvent

    data class ContentClick(
        val item: WatchLaterItem,
    ) : WatchLaterEvent
}

sealed interface WatchLaterEffect {
    data class Navigate(
        val destination: NavDestination,
    ) : WatchLaterEffect

    data class ShowMessage(
        val message: String,
    ) : WatchLaterEffect
}
