package com.zhihuminus.feature.home

import com.zhihuminus.data.FeedDisplayItem
import com.zhihuminus.navigation.NavDestination

sealed interface HomeEvent {
    data object Refresh : HomeEvent

    data object LoadMore : HomeEvent

    data class ContentClick(
        val item: FeedDisplayItem,
        val destination: NavDestination?,
    ) : HomeEvent

    data object SearchClick : HomeEvent

    data object AvatarClick : HomeEvent

    data object DismissAccountSheet : HomeEvent

    data object RequestLogin : HomeEvent

    data class ReselectTop(
        val isAtTop: Boolean,
    ) : HomeEvent

    data class UpdateUnreadCount(
        val count: Int,
    ) : HomeEvent
}

sealed interface HomeEffect {
    data class Navigate(
        val destination: NavDestination,
    ) : HomeEffect

    data class ShowMessage(
        val message: String,
    ) : HomeEffect

    data object ScrollToTop : HomeEffect

    data class OpenExternalUrl(
        val url: String,
    ) : HomeEffect
}
