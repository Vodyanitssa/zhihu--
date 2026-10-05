package com.zhihuminus.feature.follow

import com.zhihuminus.data.FeedDisplayItem

data class FollowingUser(
    val id: String,
    val urlToken: String,
    val name: String,
    val avatarUrl: String,
    val unreadCount: Int = 0,
)

data class FollowFeedPage(
    val items: List<FeedDisplayItem>,
    val nextUrl: String?,
    val isEnd: Boolean,
)

interface FollowRepository {
    suspend fun fetchFollowFeed(nextUrl: String?): FollowFeedPage

    suspend fun fetchRecentFollowingUsers(): List<FollowingUser>
}
