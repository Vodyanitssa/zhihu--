package com.zhihuminus.feature.topic

import com.zhihuminus.data.FeedDisplayItem
import com.zhihuminus.navigation.NavDestination

sealed interface TopicEvent {
    data class InitializeSection(
        val section: String,
    ) : TopicEvent

    data class SelectTab(
        val tab: TopicFeedTab,
    ) : TopicEvent

    data class SelectDiscussionSort(
        val sort: TopicDiscussionSort,
    ) : TopicEvent

    data class SelectIdeasSort(
        val sort: TopicIdeasSort,
    ) : TopicEvent

    data object LoadMore : TopicEvent

    data object Retry : TopicEvent

    data class Follow(
        val following: Boolean,
    ) : TopicEvent

    data class ContentClick(
        val item: FeedDisplayItem,
    ) : TopicEvent
}

sealed interface TopicEffect {
    data class ShowMessage(
        val message: String,
    ) : TopicEffect

    data class Navigate(
        val destination: NavDestination,
    ) : TopicEffect
}
