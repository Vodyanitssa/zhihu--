package com.zhihuminus.feature.home

import com.zhihuminus.data.FeedDisplayItem

data class HomeFeedPage(
    val items: List<FeedDisplayItem>,
    val nextUrl: String?,
    val isEnd: Boolean,
)

data class FeedExposureRecord(
    val type: String,
    val id: String,
    val attachedInfo: String? = null,
    val index: Int = 0,
)

interface HomeRepository {
    suspend fun fetchRecommendFeed(nextUrl: String?): HomeFeedPage

    suspend fun reportContentTouch(untouchedItems: List<Pair<String, String>>)

    suspend fun reportExposures(records: List<FeedExposureRecord>) {
        reportContentTouch(records.map { it.type to it.id })
    }

    suspend fun reportContentRead(type: String, id: String)

    suspend fun fetchUnreadNotificationCount(): Int

    suspend fun loadStartupSnapshot(): List<FeedDisplayItem>

    suspend fun saveStartupSnapshot(items: List<FeedDisplayItem>)

    suspend fun markUninterested(contentToken: String, contentType: String): Boolean
}
