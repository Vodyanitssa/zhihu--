package com.zhihuminus.feature.follow

import com.zhihuminus.data.FeedDisplayItem
import com.zhihuminus.navigation.NavDestination

sealed interface FollowEvent {
    data object Refresh : FollowEvent

    data object LoadMore : FollowEvent

    data class UserClick(
        val user: FollowingUser,
    ) : FollowEvent

    data class ContentClick(
        val item: FeedDisplayItem,
        val destination: NavDestination?,
    ) : FollowEvent

    data class ReselectTop(
        val isAtTop: Boolean,
    ) : FollowEvent
}

sealed interface FollowEffect {
    data class Navigate(
        val destination: NavDestination,
    ) : FollowEffect

    data class ShowMessage(
        val message: String,
    ) : FollowEffect

    data object ScrollToTop : FollowEffect

    data class OpenExternalUrl(
        val url: String,
    ) : FollowEffect
}
