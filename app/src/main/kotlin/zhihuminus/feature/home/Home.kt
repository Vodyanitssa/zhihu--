package com.zhihuminus.feature.home

import com.zhihuminus.data.FeedDisplayItem

data class HomeFeedPage(
    val items: List<FeedDisplayItem>,
    val nextUrl: String?,
    val isEnd: Boolean,
)

interface HomeRepository {
    suspend fun fetchRecommendFeed(nextUrl: String?): HomeFeedPage

    suspend fun reportContentTouch(untouchedItems: List<Pair<String, String>>)

    suspend fun reportContentRead(type: String, id: String)

    suspend fun fetchUnreadNotificationCount(): Int

    suspend fun loadStartupSnapshot(): List<FeedDisplayItem>

    suspend fun saveStartupSnapshot(items: List<FeedDisplayItem>)
}
