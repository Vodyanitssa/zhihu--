package com.zhihuminus.feature.topic

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
}

sealed interface TopicEffect {
    data class ShowMessage(
        val message: String,
    ) : TopicEffect
}
