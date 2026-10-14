package com.zhihuminus.feature.home

import com.zhihuminus.data.FeedDisplayItem
import com.zhihuminus.navigation.NavDestination

sealed interface HomeEvent {
    data object Refresh : HomeEvent

    data object LoadMore : HomeEvent

    data class ContentClick(
        val item: FeedDisplayItem,
    ) : HomeEvent

    data class ItemsExposed(
        val items: List<FeedDisplayItem>,
    ) : HomeEvent

    data object SearchClick : HomeEvent

    data object AvatarClick : HomeEvent

    data object RequestLogin : HomeEvent

    data class UpdateUnreadCount(
        val count: Int,
    ) : HomeEvent

    data class MarkUninterested(
        val item: FeedDisplayItem,
    ) : HomeEvent

    data class AddToWatchLater(
        val item: FeedDisplayItem,
    ) : HomeEvent
}

sealed interface HomeEffect {
    data class Navigate(
        val destination: NavDestination,
    ) : HomeEffect

    data class ShowMessage(
        val message: String,
    ) : HomeEffect

    data class OpenExternalUrl(
        val url: String,
    ) : HomeEffect
}
